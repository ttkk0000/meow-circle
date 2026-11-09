// Grammar reads of changed methods only; no application, type checker or compiler.
const fs = require('fs');
const path = require('path');
(async () => {
  const [modules, file] = process.argv.slice(2);
  const { Parser, Language } = require(path.join(modules, 'web-tree-sitter/tree-sitter.cjs'));
  await Parser.init({ locateFile: name => path.join(modules, 'web-tree-sitter', name) });
  const parser = new Parser();
  parser.setLanguage(await Language.load(path.join(modules, 'tree-sitter-wasms/out/tree-sitter-kotlin.wasm')));
  const source = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
  const boundaries = file.endsWith('MobileApi.kt') ? [
    ['session-save', '    private fun save(', '    suspend fun restore('],
    ['logout', '    suspend fun logout(', '    suspend fun forgetExpiredSession('],
  ] : file.endsWith('MobileStore.kt') ? [
    ['account-change', '    private fun accountChanged(', '    private fun task('],
    ['logout', '    fun logout(', '    private fun command('],
    ['authentication-completion', '    private fun finishAuthentication(', '    fun login('],
    ['completion-model', 'data class AuthCompletion(', 'data class AdoptionCommand('],
  ] : [
    ['security-views', '    fun setSecurityView(', '    /** Called only after the versioned preference'],
  ];
  const rows = [];
  for (const [label, start, end] of boundaries) {
    const from = source.indexOf(start), to = source.indexOf(end, from);
    if (from < 0 || to < 0) throw Error('Missing fragment boundary: ' + label);
    const tree = parser.parse('class StaticRead {\n' + source.slice(from, to) + '\n}');
    const errors = [];
    function visit(node) {
      if (node.type === 'ERROR' || node.isMissing) errors.push({ type: node.type, line: node.startPosition.row + 1, text: node.text.slice(0, 160) });
      for (const child of node.children) if (child.hasError || child.type === 'ERROR' || child.isMissing) visit(child);
    }
    visit(tree.rootNode); tree.delete();
    rows.push({ label, errors });
  }
  parser.delete();
  process.stdout.write(JSON.stringify({ fragments: rows }));
  if (rows.some(row => row.errors.length)) process.exitCode = 1;
})().catch(error => { process.stderr.write(String(error)); process.exitCode = 1; });
