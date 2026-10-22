// Figma Plugin API recipe; requires per-call parameters and current canvas read. Do not auto-replay.
const page=await figma.getNodeByIdAsync(PAGE_ID);await figma.setCurrentPageAsync(page);const M=[],evidence=[];
function eq(id,x){return{type:'EXPRESSION',resolvedType:'BOOLEAN',value:{expressionFunction:'EQUALS',expressionArguments:[{type:'VARIABLE_ALIAS',resolvedType:'FLOAT',value:{type:'VARIABLE_ALIAS',id}},{type:'FLOAT',resolvedType:'FLOAT',value:x}]}};}
function ne(id,x){const a=eq(id,x);a.value.expressionFunction='NOT_EQUAL';return a;}
function op(name,a){return a.length===1?a[0]:a.reduce((l,r)=>({type:'EXPRESSION',resolvedType:'BOOLEAN',value:{expressionFunction:name,expressionArguments:[l,r]}}));}
function one(condition,actions){return{type:'CONDITIONAL',conditionalBlocks:[{condition,actions}]};}
function set(k,x){return{type:'SET_VARIABLE',variableId:V[k],variableValue:{type:typeof x==='string'?'STRING':'FLOAT',resolvedType:typeof x==='string'?'STRING':'FLOAT',value:x}};}
function nav(p,k){return{type:'NODE',destinationId:T[p+'/'+k].id,navigation:'NAVIGATE',transition:null,resetScrollPosition:false,resetInteractiveComponents:false,resetVideoPosition:false};}
function clean(n){return JSON.parse(JSON.stringify(n.reactions||[])).map(r=>({trigger:r.trigger,actions:r.actions||(r.action?[r.action]:[])}));}
function prepare(s){return[set('prototype/aiOrigin',s.code),set('prototype/aiContext',s.label),one(ne(V['prototype/aiScopeGroup'],s.group),[set('prototype/aiScopeGroup',s.group),set('prototype/aiSaved',0),set('prototype/aiDraftCurrent',1),set('prototype/aiEntryLabel','如含AI生成合成内容，请主动声明')])];}
function ready(s){return op('OR',[eq(V['prototype/aiNeedsReview'],0),op('AND',[eq(V['prototype/aiNeedsReview'],1),eq(V['prototype/aiSaved'],1),eq(V['prototype/aiDraftCurrent'],1),eq(V['prototype/aiScopeGroup'],s.group)])]);}
function not(q){return{type:'EXPRESSION',resolvedType:'BOOLEAN',value:{expressionFunction:'EQUALS',expressionArguments:[q,{type:'BOOLEAN',resolvedType:'BOOLEAN',value:false}]}};}
function removeReset(x){if(!x||typeof x!=='object')return;if(Array.isArray(x)){for(let i=x.length-1;i>=0;i--)if(x[i]?.type==='SET_VARIABLE'&&x[i].variableId===V['prototype/aiNeedsReview']&&x[i].variableValue?.value===0)x.splice(i,1);else removeReset(x[i]);}else for(const v of Object.values(x))if(v&&typeof v==='object')removeReset(v);}
for(const p of PLATFORMS)for(const s of SOURCES){const root=await figma.getNodeByIdAsync(T[p+'/original:'+s.key].id),known=KNOWN.filter(x=>x.platform===p&&x.root===root.id);let instances;
let row=known.find(x=>x.kind==='entry'&&x.to==='aiChoice');row=row?await figma.getNodeByIdAsync(row.node):null;if(!row){instances=root.findAllWithCriteria({types:['INSTANCE']});row=instances.find(n=>n.name==='Legal entry / aiChoice');}if(!row)throw Error('Missing source row '+root.id);
await row.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[...prepare(s),nav(p,'aiChoice')]}]);M.push(row.id);
if(!s.publish)continue;let buttons=known.filter(x=>x.kind==='publication_guard').map(x=>x.node);if(buttons.length)buttons=await Promise.all(buttons.map(id=>figma.getNodeByIdAsync(id)));else{instances=instances||root.findAllWithCriteria({types:['INSTANCE']});buttons=instances.filter(n=>n.mainComponent?.id==='56:179'&&Object.values(n.componentProperties).some(v=>v.type==='TEXT'&&/发布|分享|发出|发送|发到/.test(v.value)));}
for(const b of buttons){let rs=clean(b);if(rs.length){removeReset(rs);}else rs=[{trigger:{type:'ON_CLICK'},actions:[...prepare(s),one(not(ready(s)),[nav(p,'aiChoice')])]}];await b.setReactionsAsync(rs);M.push(b.id);evidence.push({platform:p,root:root.id,node:b.id,source:s.key});}}
return{page:PAGE_ID,mutatedNodeIds:M,evidence};
