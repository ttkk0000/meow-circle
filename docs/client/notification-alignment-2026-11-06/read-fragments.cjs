// Read-only grammar checks for the notification changes masked by old Kotlin diagnostics.
const fs = require('fs'), path = require('path'), cp = require('child_process');
(async () => {
  const [root, modules] = process.argv.slice(2);
  const {Parser,Language}=require(path.join(modules,'web-tree-sitter/tree-sitter.cjs'));
  await Parser.init({locateFile:name=>path.join(modules,'web-tree-sitter',name)});
  const parser=new Parser();
  parser.setLanguage(await Language.load(path.join(modules,'tree-sitter-wasms/out/tree-sitter-kotlin.wasm')));
  const rows=[];
  function cut(file,label,start,end,wrap=true) {
    const source=fs.readFileSync(path.join(root,file),'utf8').replace(/\r\n/g,'\n');
    const from=source.indexOf(start),to=source.indexOf(end,from);
    if(from<0||to<0)throw Error('Missing fragment '+label);
    const tree=parser.parse((wrap?'class StaticRead {\n':'')+source.slice(from,to)+(wrap?'\n}':''),null,{bufferSize:1024*1024});
    const errors=[];
    function visit(n) {if(n.type==='ERROR'||n.isMissing){errors.push({type:n.type,text:n.text.slice(0,120)});return;} if(n.hasError)for(const c of n.children)visit(c);}
    visit(tree.rootNode);tree.delete();
    const row={file,label,errors};
    if(errors.length) {
      const before=cp.execFileSync('git',['show','HEAD:'+file],{cwd:root,encoding:'utf8',maxBuffer:4*1024*1024}).replace(/\r\n/g,'\n');
      const a=before.indexOf(start),b=before.indexOf(end,a);
      if(a<0||b<0)throw Error('Missing baseline '+label);
      const oldTree=parser.parse((wrap?'class StaticRead {\n':'')+before.slice(a,b)+(wrap?'\n}':''),null,{bufferSize:1024*1024});
      const baseline=[];
      function visitOld(n) {if(n.type==='ERROR'||n.isMissing){baseline.push({type:n.type,text:n.text.slice(0,120)});return;}if(n.hasError)for(const c of n.children)visitOld(c);}
      visitOld(oldTree.rootNode);oldTree.delete();
      row.baseline_errors=baseline;
      row.baseline_same_error_signatures=JSON.stringify(errors)===JSON.stringify(baseline);
      if(!row.baseline_same_error_signatures)process.exitCode=1;
    }
    rows.push(row);
  }
  const store='kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt';
  cut(store,'destination-state-and-canonical-drafts','sealed interface MobileDestination','class MobileStore',false);
  cut(store,'account-scope-cleanup','    private fun accountChanged(','    private fun task(');
  cut(store,'navigation-and-parent-drafts','    fun select(','    fun showLogin(');
  cut(store,'notification-route-whitelist','    fun openNotification(','    fun refreshInbox(');
  cut(store,'reply-draft-and-unknown-command-guards','    private fun canEditReply(','    private suspend fun persistCompose(');
  cut(store,'focused-comment-read-and-pagination','    fun reloadDetail(','    fun loadMessages(');
  cut(store,'send-reply-and-canonical-cleanup','    fun sendReply(','    fun markChatRead(');
  cut(store,'original-command-and-draft-recovery','    fun retryPending(','    fun addPhotos(');
  cut(store,'workspace-persistence-and-restore','    private fun replyPath(','    suspend fun media(');
  const ui='kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt';
  cut(ui,'four-state-comment-and-reused-comment-card','@Composable private fun NotificationCommentContent','@Composable\nprivate fun ApplicationDetail',false);
  cut(ui,'existing-reply-editor-with-specific-label','@Composable\nprivate fun ReplyEditor','@Composable\nprivate fun PublishScreen',false);
  cut(ui,'destination-title','private fun title(s:','private fun notificationDestination(',false);
  parser.delete();process.stdout.write(JSON.stringify({fragments:rows,type_checked:false,tests_executed:false,runtime_verified:false}));
})().catch(e=>{process.stderr.write(String(e.stack||e));process.exitCode=1;});
