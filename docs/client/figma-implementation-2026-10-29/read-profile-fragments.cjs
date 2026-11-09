// Changed-method grammar reads and explicit old Android diagnostic comparison only.
const fs = require('fs');
const path = require('path');
const cp = require('child_process');
(async () => {
  const [modules, file] = process.argv.slice(2);
  const { Parser, Language } = require(path.join(modules, 'web-tree-sitter/tree-sitter.cjs'));
  await Parser.init({ locateFile: name => path.join(modules, 'web-tree-sitter', name) });
  const parser = new Parser();
  parser.setLanguage(await Language.load(path.join(modules, 'tree-sitter-wasms/out/tree-sitter-kotlin.wasm')));
  const source = fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
  function parse(value) {
    const tree = parser.parse(value, null, { bufferSize: 1024 * 1024 });
    const errors = [];
    function visit(node) {
      if (node.type === 'ERROR' || node.isMissing) errors.push({ type: node.type, text: node.text.slice(0, 160) });
      for (const child of node.children) if (child.hasError || child.type === 'ERROR' || child.isMissing) visit(child);
    }
    visit(tree.rootNode); tree.delete(); return errors;
  }
  const report = { fragments: [] };
  function cut(label, start, end) {
    const from = source.indexOf(start), to = end ? source.indexOf(end, from) : source.length;
    if (from < 0 || to < 0) throw Error('Missing fragment: ' + label);
    report.fragments.push({ label, errors: parse('class StaticRead {\n' + source.slice(from, to) + '\n}') });
  }
  if (file.endsWith('MobileApi.kt')) {
    cut('profile-read-and-save', '    suspend fun profile(', '    suspend fun entries(');
    cut('identity-save-and-restore', '    private fun save(', '    private fun ensureStorage(');
  } else if (file.endsWith('MobileStore.kt')) {
    cut('completion-owner-model', 'data class AuthCompletion(', 'data class AdoptionCommand(');
    cut('completion-and-optional-profile', '    private fun finishAuthentication(', '    fun login(');
    cut('login-dismiss-guard', '    fun showLogin(', '    fun editAuth(');
  } else if (file.endsWith('MobileFeatureStore.kt')) {
    cut('feature-state-model', 'data class FeatureState(', 'class MobileFeatureStore(');
    cut('otp-delivery-and-countdown', '    fun clearCommandFeedback(', '    suspend fun safetyToken(');
  } else if (file.endsWith('AndroidPhotoPicker.kt')) {
    const marker = '/kmp/androidApp/';
    const relative = file.replace(/\\/g, '/').split(marker)[1];
    if (!relative) throw Error('Unexpected Android path');
    const old = cp.execFileSync('git', ['show', 'HEAD:kmp/androidApp/' + relative], { encoding: 'utf8' }).replace(/\r\n/g, '\n');
    report.errors = parse(source); report.baseline_errors = parse(old);
    report.baseline_same_error_signatures = JSON.stringify(report.errors) === JSON.stringify(report.baseline_errors);
    const from = source.indexOf('    val canPick =');
    if (from < 0) throw Error('Missing picker trigger');
    report.fragments.push({ label: 'custom-picker-trigger', errors: parse('fun staticRead() {\n' + source.slice(from)) });
    if (!report.baseline_same_error_signatures) process.exitCode = 1;
  } else throw Error('Unsupported file');
  parser.delete();
  process.stdout.write(JSON.stringify(report));
  if (report.fragments.some(row => row.errors.length)) process.exitCode = 1;
})().catch(error => { process.stderr.write(String(error)); process.exitCode = 1; });
