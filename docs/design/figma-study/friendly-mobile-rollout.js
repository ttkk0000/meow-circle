// Caller supplies ROOTS from mobile-v2-state.json. Shared controls are updated first.
const page=await figma.getNodeByIdAsync('56:25');await figma.setCurrentPageAsync(page);
const createdNodeIds=[],mutatedNodeIds=[],screens=[],fontMap=new Map();
for(const[key,id]of Object.entries(ROOTS)){const n=await figma.getNodeByIdAsync(id);if(!n||n.type!=='FRAME'||n.parent.id!==page.id)throw new Error('Missing phone root '+key);screens.push({key,n});for(const t of n.findAllWithCriteria({types:['TEXT']}))for(const s of t.getStyledTextSegments(['fontName']))fontMap.set(JSON.stringify(s.fontName),s.fontName);}
fontMap.set('Medium',{family:'Noto Sans SC',style:'Medium'});await Promise.all([...fontMap.values()].map(f=>figma.loadFontAsync(f)));
const radius=await figma.variables.getVariableByIdAsync('VariableID:56:22'),space12=await figma.variables.getVariableByIdAsync('VariableID:14:54');
const secondaryMain=await figma.getNodeByIdAsync('56:180');if(!secondaryMain||secondaryMain.type!=='COMPONENT')throw new Error('Missing secondary source');
function walk(n,fn){fn(n);if('children'in n)for(const c of n.children)walk(c,fn);}
function inInstance(n,root){for(let p=n.parent;p&&p.id!==root.id;p=p.parent)if(p.type==='INSTANCE')return true;return false;}
function reactionsOf(roots){const result={};for(const{n}of roots)walk(n,x=>{if('reactions'in x&&Array.isArray(x.reactions)&&x.reactions.length)result[x.id]=JSON.stringify(x.reactions);});return result;}
const navigationBefore=reactionsOf(screens),panelChanges=[];
for(const{key,n}of screens){for(const f of n.findAllWithCriteria({types:['FRAME']})){if(f.cornerRadius===14&&!inInstance(f,n)){for(const k of ['topLeftRadius','topRightRadius','bottomLeftRadius','bottomRightRadius'])f.setBoundVariable(k,radius);mutatedNodeIds.push(f.id);panelChanges.push({id:f.id,screen:key,name:f.name});}}}
const detail=screens.find(s=>s.key==='detail').n,footer=detail.children.find(n=>n.name==='Primary action');
let actionRow=footer.children.find(n=>n.name==='Action hierarchy / Friendly rollout');
let chat=null,primary=null;
if(!actionRow){
 primary=footer.children.find(n=>n.type==='INSTANCE');if(!primary)throw new Error('Missing detail primary');
 actionRow=figma.createAutoLayout('HORIZONTAL');footer.insertChild(0,actionRow);createdNodeIds.push(actionRow.id);actionRow.name='Action hierarchy / Friendly rollout';actionRow.resize(358,48);actionRow.layoutSizingHorizontal='FIXED';actionRow.layoutSizingVertical='HUG';actionRow.setBoundVariable('itemSpacing',space12);actionRow.fills=[];
 chat=secondaryMain.createInstance();actionRow.appendChild(chat);walk(chat,n=>createdNodeIds.push(n.id));chat.name='Button / Secondary / 先聊一聊';chat.setProperties({'Label#56:0':'先聊一聊'});chat.resize(132,48);
 actionRow.appendChild(primary);primary.setProperties({'Label#56:0':'申请领养'});primary.resize(214,48);mutatedNodeIds.push(primary.id,footer.id);
 await chat.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:ROOTS.chat,navigation:'NAVIGATE',transition:null}]}]);
}else{chat=actionRow.children.find(n=>n.type==='INSTANCE'&&n.componentProperties['Label#56:0']?.value==='先聊一聊');primary=actionRow.children.find(n=>n.type==='INSTANCE'&&n.componentProperties['Label#56:0']?.value==='申请领养');}
const counts={},fonts=new Set(),images=[],missingFonts=[],badButtonStyles=[],buttonChecks=[],targets=new Set();let totalNodes=0,reactionCount=0;
const types={'56:179':'Primary','56:180':'Secondary','56:181':'Quiet','56:182':'Disabled'};
for(const{key,n}of screens){
 if(n.width!==390||n.height!==844)throw new Error('Phone dimension changed '+key);
 const sourceButtons=[];for(const i of n.findAllWithCriteria({types:['INSTANCE']})){const m=await i.getMainComponentAsync();if(!types[m?.id])continue;const text=i.findAllWithCriteria({types:['TEXT']})[0];const expectedSize=types[m.id]==='Quiet'?14:16;const valid=i.cornerRadius===16&&i.height===48&&text.fontName.family==='Noto Sans SC'&&text.fontName.style==='Medium'&&text.fontSize===expectedSize;if(!valid)badButtonStyles.push({id:i.id,screen:key,mainId:m.id,radius:i.cornerRadius,height:i.height,font:text.fontName,size:text.fontSize});sourceButtons.push({id:i.id,kind:types[m.id],label:i.componentProperties['Label#56:0']?.value,width:i.width,height:i.height});}
 buttonChecks.push({screen:key,id:n.id,buttonCount:sourceButtons.length,kinds:[...new Set(sourceButtons.map(x=>x.kind))],minWidth:sourceButtons.length?Math.min(...sourceButtons.map(x=>x.width)):null});
 walk(n,x=>{totalNodes++;counts[x.type]=(counts[x.type]||0)+1;if(x.type==='TEXT'){for(const s of x.getStyledTextSegments(['fontName']))fonts.add(s.fontName.family);if(x.hasMissingFont)missingFonts.push(x.id);}if('fills'in x&&Array.isArray(x.fills)&&x.fills.some(p=>p.type==='IMAGE'))images.push({id:x.id,name:x.name,type:x.type,width:x.width,height:x.height});if('reactions'in x&&Array.isArray(x.reactions))for(const r of x.reactions){reactionCount++;for(const a of r.actions||[])if(a.type==='NODE'&&a.destinationId)targets.add(a.destinationId);}});
}
const navigationAfter=reactionsOf(screens);const preserved=Object.keys(navigationBefore).every(id=>navigationAfter[id]===navigationBefore[id]);if(!preserved)throw new Error('Existing navigation source changed');
const invalidTargets=[];for(const id of targets)if(!await figma.getNodeByIdAsync(id))invalidTargets.push(id);
const badFamilies=[...fonts].filter(f=>!['Noto Sans SC','Manrope'].includes(f));if(badButtonStyles.length||missingFonts.length||badFamilies.length||invalidTargets.length)throw new Error('Phone acceptance failed '+JSON.stringify({badButtonStyles,missingFonts,badFamilies,invalidTargets}));
return{createdNodeIds,mutatedNodeIds,roots:ROOTS,pageId:page.id,screenCount:screens.length,panelChangeCount:panelChanges.length,detailActions:{rowId:actionRow.id,primaryId:primary.id,secondaryId:chat.id},buttonChecks,badButtonStyles,nodeTypeCounts:counts,totalNodeCount:totalNodes,imageNodes:images,fontFamilies:[...fonts],missingFonts,badFamilies,navigation:{beforeSourceCount:Object.keys(navigationBefore).length,afterSourceCount:Object.keys(navigationAfter).length,reactionCount,allExistingSourcesPreserved:preserved,invalidTargets,addedChatSourceId:chat.id},status:'all phone frames aligned / visual review pending'};
