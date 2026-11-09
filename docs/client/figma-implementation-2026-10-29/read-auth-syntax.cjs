// Isolated grammar diagnostics only; never loads the application or compiler.
const fs = require('fs');
const path = require('path');
const cp = require('child_process');
(async () => {
  const modules = process.argv[2];
  const { Parser, Language } = require(path.join(modules, 'web-tree-sitter/tree-sitter.cjs'));
  await Parser.init({ locateFile: file => path.join(modules, 'web-tree-sitter', file) });
  const language = await Language.load(path.join(modules, 'tree-sitter-wasms/out/tree-sitter-kotlin.wasm'));
  const parser = new Parser(); parser.setLanguage(language);
  let source = fs.readFileSync(process.argv[3], 'utf8').replace(/\r\n/g, '\n');
  if (process.argv[3].endsWith('MobileFeatureStore.kt')) {
    source = 'class StaticRead {\n' + source.slice(source.indexOf('    fun clearCommandFeedback('), source.indexOf('    suspend fun safetyToken(')) + '\n}';
  } else if (process.argv[3].endsWith('MobileStore.kt')) {
    source = 'class StaticRead {\n' + source.slice(source.indexOf('    fun refreshCapabilities('), source.indexOf('    fun select(')) + '\n}';
  }
  const tree = parser.parse(source, null, { bufferSize: 1024 * 1024 });
  const problems = [];
  function visit(node) {
    if (node.type === 'ERROR' || node.isMissing) problems.push({ type: node.type, start: node.startPosition, end: node.endPosition, text: node.text.slice(-300) });
    for (const child of node.children) if (child.hasError || child.type === 'ERROR' || child.isMissing) visit(child);
  }
  visit(tree.rootNode);
  const report = { problems, top: tree.rootNode.children.map(n => ({ type: n.type, start: n.startPosition, end: n.endPosition })) };
  if (problems.length && process.argv[3].endsWith('MobileStore.kt')) {
    const before = cp.execFileSync('git', ['show', 'HEAD:kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt'], { encoding: 'utf8' }).replace(/\r\n/g, '\n');
    const oldSource = 'class StaticRead {\n' + before.slice(before.indexOf('    private fun refreshCapabilities('), before.indexOf('    fun select(')) + '\n}';
    const oldTree = parser.parse(oldSource);
    const oldProblems = [];
    function oldVisit(node) {
      if (node.type === 'ERROR' || node.isMissing) oldProblems.push({ type: node.type, text: node.text.slice(-300) });
      for (const child of node.children) if (child.hasError || child.type === 'ERROR' || child.isMissing) oldVisit(child);
    }
    oldVisit(oldTree.rootNode); oldTree.delete();
    const same = JSON.stringify(problems.map(({type,text}) => ({type,text}))) === JSON.stringify(oldProblems);
    report.baseline_same_error_signatures = same;
    report.baseline_problems = oldProblems;
    if (!same) process.exitCode = 1;
  } else if (problems.length) process.exitCode = 1;
  process.stdout.write(JSON.stringify(report, null, 2));
  tree.delete(); parser.delete();
})().catch(error => { process.stderr.write(String(error)); process.exitCode = 1; });
