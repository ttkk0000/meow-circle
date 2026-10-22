// Updates only the review wrapper from the returned ledger; source phone drafts stay intact.
const page=await figma.getNodeByIdAsync('56:26');await figma.setCurrentPageAsync(page);
const board=await figma.getNodeByIdAsync('81:192'),row=await figma.getNodeByIdAsync('81:195');
if(!board||!row||row.children.length)throw new Error('Missing or populated review wrapper; inspect saved state before retry.');
const createdNodeIds=[],mutatedNodeIds=[board.id,row.id],implicitRemovedNodeIds=[],styleJobs=[];
const roots={},changes=[],fontMap=new Map(),sources=[];
for(const[key,id,title]of[['home','56:27','01 / 社区首页'],['detail','56:37','02 / 领养详情'],['apply','56:39','03 / 领养申请']]){
 const source=await figma.getNodeByIdAsync(id);if(!source||source.type!=='FRAME')throw new Error('Missing source '+id);
 sources.push({key,id,title,source});
 for(const t of source.findAllWithCriteria({types:['TEXT']}))for(const s of t.getStyledTextSegments(['fontName']))fontMap.set(JSON.stringify(s.fontName),s.fontName);
}
for(const f of [{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'}])fontMap.set(JSON.stringify(f),f);
await Promise.all([...fontMap.values()].map(f=>figma.loadFontAsync(f)));
const variableIds={ink:'VariableID:14:39',muted:'VariableID:14:40',gap12:'VariableID:14:54',gap16:'VariableID:14:55'};
const V={};for(const[k,id]of Object.entries(variableIds))V[k]=await figma.variables.getVariableByIdAsync(id);
const paint=v=>figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:0,g:0,b:0}},'color',v);
function walk(n,fn){fn(n);if('children'in n)for(const c of n.children)walk(c,fn);}
function text(parent,value,style,color,width){const t=figma.createText();parent.appendChild(t);t.fontName={family:'Noto Sans SC',style:'Regular'};t.resize(width,24);t.textAutoResize='HEIGHT';t.characters=value;t.fills=[paint(color)];styleJobs.push(t.setTextStyleIdAsync(style));return t;}
const oldMap={'56:179':'70:172','56:180':'70:173','56:181':'70:174','56:182':'70:177'};
const mains={};for(const id of Object.values(oldMap))mains[id]=await figma.getNodeByIdAsync(id);
const initialIds=new Set();
for(const{key,id,title,source}of sources){
 const col=figma.createAutoLayout('VERTICAL');row.appendChild(col);col.name='Review / '+key;col.resize(390,100);col.layoutSizingHorizontal='FIXED';col.layoutSizingVertical='HUG';col.setBoundVariable('itemSpacing',V.gap16);col.fills=[];col.clipsContent=false;
 text(col,title,'S:2df419e6e43a4b1ea6eb387c9368587ce8ab132b,',V.ink,390);
 const screen=source.clone();col.appendChild(screen);screen.name='Friendly phone / '+key;screen.resize(390,844);screen.layoutSizingHorizontal='FIXED';screen.layoutSizingVertical='FIXED';screen.setExplicitVariableModeForCollection('VariableCollectionId:70:148','70:0');roots[key]=screen.id;
 walk(screen,n=>initialIds.add(n.id));
 for(const n of screen.findAllWithCriteria({types:['INSTANCE']})){
  const m=await n.getMainComponentAsync();if(!oldMap[m?.id])continue;
  const previous=m.id,label=Object.values(n.componentProperties).find(p=>p.type==='TEXT')?.value||'';
  const width=n.width;n.swapComponent(mains[oldMap[previous]]);n.setProperties({'Label#70:0':label});n.resize(width,48);
  changes.push({id:n.id,screen:key,previousMainId:previous,mainId:oldMap[previous],label,width,height:n.height});
 }
 if(key==='detail'){
  const footer=screen.children.find(n=>n.name==='Primary action');
  const primary=footer.children.find(n=>n.type==='INSTANCE');if(!primary)throw new Error('Missing detail primary');
  const actions=figma.createAutoLayout('HORIZONTAL');footer.insertChild(0,actions);actions.name='Action hierarchy / chosen Friendly';actions.resize(358,48);actions.layoutSizingHorizontal='FIXED';actions.layoutSizingVertical='HUG';actions.setBoundVariable('itemSpacing',V.gap12);actions.fills=[];
  const secondary=mains['70:173'].createInstance();actions.appendChild(secondary);secondary.name='Button / Friendly / Secondary';secondary.setProperties({'Label#70:0':'先聊一聊'});secondary.resize(132,48);
  actions.appendChild(primary);primary.setProperties({'Label#70:0':'申请领养'});primary.resize(214,48);
  const change=changes.find(c=>c.id===primary.id);change.label='申请领养';change.width=214;
  changes.push({id:secondary.id,screen:key,mainId:'70:173',label:'先聊一聊',width:132,height:48});
 }
 const all=[];walk(screen,n=>all.push(n));for(const n of all)if('reactions'in n&&Array.isArray(n.reactions)&&n.reactions.length)await n.setReactionsAsync([]);
}
// Review screens are nested in a visual board, not top-level prototype destinations.
// Keep all interactions absent rather than leaving routes into older mixed-style drafts.
text(board,'本轮仅确认按钮在页面中的应用；输入框、筛选、导航与完整信息层级仍待评审。','S:4ec5595d1cb39f780f1ba2061e632d77abfda629,',V.ink,1234);
text(board,'学习素材与静态字段，非真实宠物档案。本板只供视觉评审；聊天、筛选、填写、提交及页面跳转未接入。','S:e31250e9288e02c9954a72976177f3677be7fbd6,',V.muted,1234);
await Promise.all(styleJobs);board.placeholder=false;
const liveIds=new Set(),counts={},images=[],families=new Set(),missingFonts=[];let reactionCount=0;
walk(board,n=>{liveIds.add(n.id);if(!mutatedNodeIds.includes(n.id))createdNodeIds.push(n.id);counts[n.type]=(counts[n.type]||0)+1;if(n.type==='TEXT'){for(const s of n.getStyledTextSegments(['fontName']))families.add(s.fontName.family);if(n.hasMissingFont)missingFonts.push(n.id);}if('fills'in n&&Array.isArray(n.fills)&&n.fills.some(p=>p.type==='IMAGE'))images.push({id:n.id,name:n.name,type:n.type,width:n.width,height:n.height});if('reactions'in n&&Array.isArray(n.reactions))reactionCount+=n.reactions.length;});
for(const id of initialIds)if(!liveIds.has(id))implicitRemovedNodeIds.push(id);
const badFonts=[...families].filter(x=>!['Noto Sans SC','Manrope'].includes(x));if(badFonts.length||missingFonts.length)throw new Error('Font acceptance failed');
return{createdNodeIds,mutatedNodeIds,implicitRemovedNodeIds,boardId:board.id,rowId:row.id,pageId:page.id,roots,sourceRoots:Object.fromEntries(sources.map(s=>[s.key,s.id])),sourceScreensModified:false,componentChanges:changes,width:board.width,height:board.height,nodeTypeCounts:counts,totalNodeCount:liveIds.size,imageNodes:images,fontFamilies:[...families],missingFonts,badFonts,reactionCount,selectedDirection:'Friendly',buttonLabelProperty:'Label#70:0',status:'composed / needs visual review'};
