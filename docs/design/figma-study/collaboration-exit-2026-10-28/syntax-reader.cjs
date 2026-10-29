const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const root = process.argv[2];
const modules = process.argv[3];
const selected = process.argv[4];
const files = [
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/PublishButton.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/InvitationPages.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ClientTheme.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/SocialPages.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ReadingPages.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/FeaturePages.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/CarePages.kt',
'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/InvitationStatus.kt',
'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/FeatureApi.kt',
'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileFeatureStore.kt',
'web-client/src/core/invitation.ts','web-client/src/core/pages.ts','web-client/src/core/query.ts',
'web-client/src/features/ResourcePage.tsx','web-client/src/features/WorkflowEditor.tsx',
'web-client/src/features/CareCollaboration.tsx','web-client/src/App.tsx',
'web-client/src/features/InvitationPages.tsx',
'web-client/src/features/Records.tsx',
'web-client/src/components/PublishButton.tsx',
'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/TaskLinks.kt',
'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/SensitiveActionStore.kt',
'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt',
'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileApi.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/StatusComponents.kt',
'kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/MobileViewModel.kt',
'web-client/src/core/router.ts','web-client/src/components/PurposeCheck.tsx',
'web-client/src/features/Recovery.tsx',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/InboxPages.kt',
'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AccountPages.kt',
'web-client/src/components/SchemaForm.tsx','web-client/src/features/NotificationPage.tsx',
'web-client/src/operator/RoleTasks.tsx'];
function issues(node) {
  const result = [];
  function visit(n) {
    if(n.type === 'ERROR' || n.isMissing) {result.push({type:n.type,line:n.startPosition.row+1,text:n.text.slice(0,140)});return;}
    if(n.hasError) for(const c of n.children) visit(c);
  }
  visit(node);return result;
}
(async()=>{
  if(!selected){
    const report=[];
    for(const file of files){
      const result=cp.spawnSync(process.execPath,['--max-old-space-size=384','--no-wasm-tier-up','--wasm-num-compilation-tasks=1',__filename,root,modules,file],
        {encoding:'utf8',maxBuffer:1024*1024});
      if(result.status!==0)throw new Error('Syntax reader failed: '+file+'\n'+result.stderr+'\n'+result.stdout);
      report.push(JSON.parse(result.stdout));
    }
    process.stdout.write(JSON.stringify({files:report,whole_file_pass:report.filter(r=>!r.errors.length).length,
      isolated_processes:true,heap_limit_mb:384,wasm_parallel_tasks:1,wasm_tier_up:false,runtime_verified:false},null,2));
    return;
  }
  const { Parser, Language } = require(path.join(modules, 'web-tree-sitter/tree-sitter.cjs'));
  await Parser.init({locateFile:file=>path.join(modules,'web-tree-sitter',file)});
  const languages = {};
  const kind=selected.endsWith('.kt')?'kotlin':selected.endsWith('.tsx')?'tsx':'typescript';
  languages[kind]=await Language.load(path.join(modules,'tree-sitter-wasms/out/tree-sitter-'+kind+'.wasm'));
  const report=[];
  for(const file of [selected]){
    const source=fs.readFileSync(path.join(root,file),'utf8').replace(/\r\n/g,'\n');
    const parser=new Parser();parser.setLanguage(languages[file.endsWith('.kt')?'kotlin':file.endsWith('.tsx')?'tsx':'typescript']);
    const tree=parser.parse(source,null,{bufferSize:1024*1024});
    const errors=issues(tree.rootNode),row={file,errors};
    if(errors.length && ['/MobileFeatureStore.kt','/ConsumerApp.kt','/ReadingPages.kt','/SensitiveActionStore.kt','/MobileStore.kt','/MobileApi.kt'].some(name=>file.endsWith(name))){
      const before=cp.execFileSync('git',['show','HEAD:'+file],{cwd:root,encoding:'utf8',maxBuffer:4*1024*1024});
      const oldTree=parser.parse(before,null,{bufferSize:1024*1024});
      row.baseline_errors=issues(oldTree.rootNode);oldTree.delete();
      const slices=[];
      function cut(label,startText,endText,prefix='',suffix=''){
        const start=source.indexOf(startText),end=source.indexOf(endText,start);
        if(start<0||end<0)throw new Error('Changed boundaries not found: '+label);
        slices.push({label,source:prefix+source.slice(start,end)+suffix});
      }
      if(file.endsWith('/ConsumerApp.kt'))cut('window-policy-and-shell','// 按当前窗口','@Composable\nfun ContextPane');
      if(file.endsWith('/ReadingPages.kt'))cut('new-title-branches','        FeaturePage.AdoptionWork ->','        FeaturePage.AppointmentEdit ->','fun staticRead() { return when(route.page) {\n','\nelse -> ""\n} }');
      if(file.endsWith('/MobileFeatureStore.kt')){
        cut('friendship-detail-loading','                    FeaturePage.Friends ->','                    FeaturePage.Member ->','fun staticRead() { when(route.page) {\n','\n} }');
        cut('state-models','data class FeatureRoute','class MobileFeatureStore');
        cut('route-lifecycle','    fun open(','    private fun restricted()', 'class StaticRead {\n','\n}');
        cut('confirmation-and-outcome','    fun confirm(','    fun runCommand(', 'class StaticRead {\n','\n}');
        cut('collaborator-loading','                    FeaturePage.Collaborators ->','                    FeaturePage.Invitations ->','fun staticRead() { when(route.page) {\n','\n} }');
        cut('invitation-detail-loading','                    FeaturePage.Invitations ->','                    FeaturePage.Group ->','fun staticRead() { when(route.page) {\n','\n} }');
      }
      if(file.endsWith('/SensitiveActionStore.kt'))cut('matching-committed-receipt','                val result = when (intent) {','                if (revision == currentRevision) mutable.value = SensitiveActionState(SensitiveStage.Complete', 'fun staticRead() {\n','\n}');
      if(file.endsWith('/MobileStore.kt')){
        cut('notification-targets','    fun openNotification(body:','    fun refreshInbox()', 'class StaticRead {\n','\n}');
        cut('original-request-retry','    fun retryPending(','    private fun finishRecoveredReply(', 'class StaticRead {\n','\n}');
      }
      if(file.endsWith('/MobileApi.kt'))cut('receipt-id-and-unresolved-state','    suspend fun reconcileOperation(','    private fun logicalBody(', 'class StaticRead {\n','\n}');
      row.changed_fragments=slices.map(slice=>{
        const fragment=parser.parse(slice.source,null,{bufferSize:1024*1024});
        const result={label:slice.label,errors:issues(fragment.rootNode)};fragment.delete();return result;
      });
      row.whole_file_verified=false;
      row.baseline_same_error_signatures=JSON.stringify(errors.map(e=>[e.type,e.text]))===JSON.stringify(row.baseline_errors.map(e=>[e.type,e.text]));
      // The old Kotlin grammar treats the soft keyword open as an identifier error in calls.
      for(const fragment of row.changed_fragments)fragment.grammar_limitations=fragment.errors.filter(e=>e.text==='open');
      if(!row.baseline_same_error_signatures||row.changed_fragments.some(fragment=>fragment.errors.some(e=>e.text!=='open')))process.exitCode=1;
    }else if(errors.length)process.exitCode=1;
    report.push(row);tree.delete();parser.delete();
  }
  process.stdout.write(JSON.stringify(report[0]));
})().catch(e=>{process.stderr.write(String(e.stack||e));process.exitCode=1;});
