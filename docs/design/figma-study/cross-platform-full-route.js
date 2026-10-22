// Caller: PAGE_ID, ROOTS, SOURCE_MAP, REVIEW_IDS, NEW_LINKS. Read clone IDs, never phone nodes.
const page=await figma.getNodeByIdAsync(PAGE_ID);await figma.setCurrentPageAsync(page);
const mutatedNodeIds=[],unmapped=[],NEW_LINKS=[],counts={nodes:0,reactions:0,local:0,crossModule:0,back:0,selfSkipped:0};
function rewrite(actions,platform,rootId,rootPageId){const out=[];for(const a of actions||[]){
 if(a.type==='URL'&&a.url?.includes('3OFOC3vu18Kx9sRPjummOC')){const m=a.url.match(/[?&]node-id=([^&]+)/);const id=m?decodeURIComponent(m[1]).replace('-',':'):null;const dest=SOURCE_MAP[platform]?.[id];if(dest){if(dest.id===rootId){counts.selfSkipped++;continue;}if(dest.pageId===rootPageId){out.push({type:'NODE',destinationId:dest.id,navigation:'NAVIGATE',transition:null});counts.local++;}else{out.push({type:'URL',url:'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id='+dest.id.replace(':','-')});counts.crossModule++;}continue;}}
 if(a.type==='CONDITIONAL'){out.push({...a,conditionalBlocks:a.conditionalBlocks.map(b=>({...b,actions:rewrite(b.actions,platform,rootId,rootPageId)}))});continue;}
 if(a.type==='NODE'&&a.destinationId&&a.navigation!=='CHANGE_TO'){
  if(DEST_PAGE_BY_ID[a.destinationId]){if(a.destinationId===rootId){counts.selfSkipped++;continue;}if(DEST_PAGE_BY_ID[a.destinationId]===rootPageId)out.push(a);else out.push({type:'URL',url:'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id='+a.destinationId.replace(':','-')});continue;}
  const dest=SOURCE_MAP[platform]?.[a.destinationId];
  if(dest){if(dest.id===rootId){counts.selfSkipped++;continue;}if(dest.pageId===rootPageId){out.push({...a,destinationId:dest.id});counts.local++;}else{out.push({type:'URL',url:'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id='+dest.id.replace(':','-')});counts.crossModule++;}continue;}
  if(REVIEW_IDS.includes(a.destinationId)){out.push({type:'URL',url:'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id=301-27'});counts.crossModule++;continue;}
  unmapped.push({platform,rootId,destination:a.destinationId,navigation:a.navigation});out.push(a);continue;
 }
 out.push(a);
 }return out;}
for(const {id,platform}of ROOTS){const root=await figma.getNodeByIdAsync(id);if(!root||root.parent.id!==DEST_PAGE_BY_ID[id])throw Error('Wrong route root '+id);
 const ui=root.findAllWithCriteria({types:['FRAME','INSTANCE','TEXT']});
 for(const n of ui.filter(n=>n.name.startsWith('Navigate / ')||n.name==='Back / preserve current task'))NEW_LINKS.push(n.name.startsWith('Navigate / ')?{id:n.id,key:n.name.split(' → ').pop(),platform}:{id:n.id,back:true,platform});
 if(typeof ROUTE_SHELL_ONLY!=='undefined'&&ROUTE_SHELL_ONLY)continue;
 const nodes=[root,...ui.filter(n=>'reactions'in n&&n.reactions.length>0)];
 for(const n of nodes){if(!('reactions'in n)||!n.reactions.length)continue;counts.nodes++;
 const raw=JSON.parse(JSON.stringify(n.reactions)),before=JSON.stringify(raw),after=raw.map(r=>({trigger:r.trigger,actions:rewrite(r.actions?.length?r.actions:r.action?[r.action]:[],platform,id,root.parent.id)})).filter(r=>r.actions.length);
 if(before!==JSON.stringify(after)){n.reactions=after;mutatedNodeIds.push(n.id);}counts.reactions+=after.length;
 }
}
for(const l of NEW_LINKS){const n=await figma.getNodeByIdAsync(l.id);if(!n)throw Error('Missing new navigation '+l.id);let root=n;while(root.parent&&root.parent.type!=='PAGE')root=root.parent;let a;if(l.back){if(n.reactions.length)continue;a={type:'BACK'};counts.back++;}else{const d=Object.values(SOURCE_MAP[l.platform]).find(x=>x.key===l.key);if(!d)throw Error('Missing destination '+l.key);a=d.pageId===root.parent.id?{type:'NODE',destinationId:d.id,navigation:'NAVIGATE',transition:null}:{type:'URL',url:'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id='+d.id.replace(':','-')};if(root.id===d.id)continue;}
 n.reactions=[{trigger:{type:'ON_CLICK'},actions:[a]}];mutatedNodeIds.push(n.id);
}
function pack(ids){const groups={};for(const id of new Set(ids)){const i=id.lastIndexOf(':'),p=id.slice(0,i);(groups[p]??=[]).push(+id.slice(i+1));}return Object.fromEntries(Object.entries(groups).map(([p,ns])=>{ns.sort((a,b)=>a-b);return[p,{start:ns[0],deltaBase36:ns.slice(1).map((n,i)=>(n-ns[i]).toString(36)).join('.')}];}));}
return {pageId:PAGE_ID,mutatedNodeIdsCodec:'prefix-start-base36-delta-v1',mutatedNodeIdsByPrefix:pack(mutatedNodeIds),counts,unmapped:[...new Map(unmapped.map(x=>[JSON.stringify(x),x])).values()]};
