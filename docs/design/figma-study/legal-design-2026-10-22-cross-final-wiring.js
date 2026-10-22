// Canonical native reaction binding; requires PAGE_ID, PLATFORMS, SCREENS, T, V, CONSENT and SOURCES. Read canvas before replay.
const page=await figma.getNodeByIdAsync(PAGE_ID);await figma.setCurrentPageAsync(page);
await Promise.all([{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'}].map(f=>figma.loadFontAsync(f)));
const M=[],evidence=[];
function eq(id,x){return{type:'EXPRESSION',resolvedType:'BOOLEAN',value:{expressionFunction:'EQUALS',expressionArguments:[{type:'VARIABLE_ALIAS',resolvedType:'FLOAT',value:{type:'VARIABLE_ALIAS',id}},{type:'FLOAT',resolvedType:'FLOAT',value:x}]}};}
function ne(id,x){const a=eq(id,x);a.value.expressionFunction='NOT_EQUAL';return a;}
function op(name,a){return a.length===1?a[0]:a.reduce((l,r)=>({type:'EXPRESSION',resolvedType:'BOOLEAN',value:{expressionFunction:name,expressionArguments:[l,r]}}));}
function not(q){return{type:'EXPRESSION',resolvedType:'BOOLEAN',value:{expressionFunction:'EQUALS',expressionArguments:[q,{type:'BOOLEAN',resolvedType:'BOOLEAN',value:false}]}};}
function set(id,x){const type=typeof x==='string'?'STRING':typeof x==='number'?'FLOAT':'COLOR';return{type:'SET_VARIABLE',variableId:id,variableValue:{type,resolvedType:type,value:x}};}
function nav(p,k){const t=T[p+'/'+k];if(!t)throw Error('Missing target '+p+'/'+k);return{type:'NODE',destinationId:t.id,navigation:'NAVIGATE',transition:null,resetScrollPosition:false,resetInteractiveComponents:false,resetVideoPosition:false};}
function one(condition,actions){return{type:'CONDITIONAL',conditionalBlocks:[{condition,actions}]};}
function foot(r){return r.findAllWithCriteria({types:['FRAME']}).find(n=>n.name==='Persistent task actions')?.children.find(n=>n.type==='INSTANCE');}
function rgba(name){let v=byId[CONFIG.variables[name]],x=v.valuesByMode['56:0']||Object.values(v.valuesByMode)[0];for(let i=0;x?.type==='VARIABLE_ALIAS'&&i<8;i++){v=byId[x.id];x=v.valuesByMode['56:0']||Object.values(v.valuesByMode)[0];}return{r:x.r,g:x.g,b:x.b,a:1};}
function clean(n){return JSON.parse(JSON.stringify(n.reactions||[])).map(r=>({trigger:r.trigger,actions:r.actions||(r.action?[r.action]:[])}));}
const pending=Object.fromEntries(Object.entries({aiChecking:'legal/ai',authorityChecking:'legal/authority',dataChecking:'legal/erasure',rightsChecking:'legal/rights',contractChecking:'legal/contract',rightsCounterChecking:'legal/counter',rightsSupplementChecking:'legal/rightsAppend',dataAppendChecking:'legal/dataAppend'}).map(([id,key])=>[id,V['flow/'+key]]));
function submit(p,key,extra=[]){return[...extra,...(pending[key]?[set(pending[key],0)]:[]),nav(p,key)];}
for(const p of PLATFORMS) {
 for(const s of SCREENS){const root=await figma.getNodeByIdAsync(T[p+'/'+s.id].id),b=foot(root);if(!b)throw Error('Missing footer '+s.id);let a=[];
 if(s.eventGate){const ev=V['flow/'+s.eventGate.key];for(const[value,to]of Object.entries(s.eventGate.outcomes)){if(s.id==='aiChecking'&&Number(value)===1){for(const [choice,label]of [[1,'已声明包含AI生成／合成'],[2,'已声明未使用生成合成；展示仍按适用核验结果']])a.push(one(op('AND',[eq(ev,1),eq(V['prototype/aiProposed'],choice)]),[set(V['prototype/aiSaved'],1),set(V['prototype/aiDraftCurrent'],1),set(V['prototype/aiNeedsReview'],1),set(V['prototype/aiLabel'],label),set(V['prototype/aiEntryLabel'],label),nav(p,to)]));a.push(one(op('AND',[eq(ev,1),ne(V['prototype/aiProposed'],1),ne(V['prototype/aiProposed'],2)]),[nav(p,'aiChoice')]));}else a.push(one(eq(ev,Number(value)),[nav(p,to)]));}}
 else if(s.versionGate){const c=CONSENT[s.id],scope=V['flow/'+s.versionGate.key],actor=V['actor/'+s.id+'/authorized'],good=[eq(c,1),eq(scope,1),eq(actor,1)],bad=[ne(scope,1),ne(actor,1)];if(s.id==='contractFinal')for(const key of ['prototype/contract/adult','prototype/contract/currentPhone','prototype/contract/role','prototype/contract/sourceAuthority','prototype/contract/cityAdmission','prototype/contractKindSelected']){good.push(eq(V[key],1));bad.push(ne(V[key],1));}a=[one(op('AND',good),submit(p,s.versionGate.valid)),one(op('AND',[eq(c,1),op('OR',bad)]),[nav(p,s.versionGate.invalid)])];}
 else if(['aiChoice','aiResult','aiRestriction'].includes(s.id)){a=SOURCES.map(src=>one(eq(V['prototype/aiOrigin'],src.code),[nav(p,'original:'+src.key)]));a.push(one(op('AND',SOURCES.map(src=>ne(V['prototype/aiOrigin'],src.code))),[nav(p,'original:composePost')]));}
 else if(s.id==='contractReceipt'){a=[one(eq(V['prototype/contractRelation'],1),[nav(p,'original:detail')]),one(eq(V['prototype/contractRelation'],2),[nav(p,'original:saleDetail')]),one(op('AND',[ne(V['prototype/contractRelation'],1),ne(V['prototype/contractRelation'],2)]),[nav(p,'contractSelect')])];}
 else if(s.footer.requires){const extra=['aiReview','aiClearReview'].includes(s.id)?[set(V['prototype/aiProposed'],s.id==='aiReview'?1:2)]:[];a=[one(eq(CONSENT[s.id],1),submit(p,s.footer.to,extra))];}
 else {const extra=['contractAdopt','contractSale'].includes(s.id)?[set(V['prototype/contractRelation'],s.id==='contractAdopt'?1:2),set(V['prototype/contractKindSelected'],1),set(V['prototype/contractType'],s.id==='contractAdopt'?'无偿领养／可选试养 · 当前版本示例':'出售／有偿转让 · 当前版本示例')]:[];a=[...extra,nav(p,s.footer.to)];if(s.staffOnly)a=[one(eq(V['prototype/staffAuthorized'],1),a),one(ne(V['prototype/staffAuthorized'],1),[nav(p,'legalCenter')])];}
 await b.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:a}]);M.push(b.id);

 const body=root.findAllWithCriteria({types:['FRAME']}).find(n=>n.name==='Task content');if(!body)throw Error('Missing content');
 for(const item of s.items||[]){if(item.type!=='row')continue;const row=body.findAllWithCriteria({types:['INSTANCE']}).find(n=>Object.values(n.componentProperties).some(v=>v.type==='TEXT'&&v.value===item.label));if(!row)throw Error('Missing row '+s.id+'/'+item.label);await row.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[nav(p,item.to)]}]);M.push(row.id);}
 const back=root.findAllWithCriteria({types:['FRAME']}).find(n=>n.name==='Back');if(back){await back.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:['aiChoice','aiResult','aiRestriction'].includes(s.id)?a:[nav(p,s.back||'legalCenter')]}]);M.push(back.id);}
 evidence.push({platform:p,key:s.id,footer:b.id,branches:a.length});
 }
}
return{page:PAGE_ID,mutatedNodeIds:[...new Set(M)],footerCount:evidence.length,evidence};
