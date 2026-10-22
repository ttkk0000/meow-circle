// Caller supplies BASE (original study state), MOBILE, PALETTES, and the supplied variants helper.
const createdNodeIds=[],mutatedNodeIds=[],createdVariableIds=[],styleJobs=[];
const fonts=[{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'ExtraBold'}];
await Promise.all(fonts.map(f=>figma.loadFontAsync(f)));
const page=await figma.getNodeByIdAsync(MOBILE.pages.guide);await figma.setCurrentPageAsync(page);
if(page.children.some(n=>n.name==='M&D / Button directions 01-02'))throw new Error('Comparison already exists; read the saved ledger before editing.');
const previousBounds=page.children.map(n=>({right:n.x+n.width,bottom:n.y+n.height}));
const left=446,top=Math.max(...previousBounds.map(x=>x.bottom))+240,assetsX=Math.max(...previousBounds.map(x=>x.right))+240;
const vars=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(vars.map(v=>[v.id,v]));
const G=Object.fromEntries(Object.entries({...BASE.geometry,'radius/16':MOBILE.radius16}).map(([k,id])=>[k,byId[id]]));
const neutral=Object.fromEntries(Object.entries(BASE.variables).map(([k,id])=>[k,byId[id]]));
const track=n=>(createdNodeIds.push(n.id),n);
function walk(n,fn){fn(n);if('children'in n)for(const c of n.children)walk(c,fn);}
function p(v){return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:0,g:0,b:0}},'color',v);}
function space(n,key,value){if(G['space/'+value])n.setBoundVariable(key,G['space/'+value]);else n[key]=value;}
function radius(n,value){for(const key of ['topLeftRadius','topRightRadius','bottomLeftRadius','bottomRightRadius'])n.setBoundVariable(key,G['radius/'+value]);}
function fixed(n,w,h){n.resize(w,h);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='FIXED';return n;}
function box(parent,name,w,padding=0,gap=16,bg=null,r=null,direction='VERTICAL'){
 const n=track(figma.createAutoLayout(direction));parent.appendChild(n);n.name=name;n.resize(w,64);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';
 for(const key of ['paddingLeft','paddingRight','paddingTop','paddingBottom'])space(n,key,padding);space(n,'itemSpacing',gap);n.fills=bg?[p(neutral[bg])]:[];n.clipsContent=false;if(r)radius(n,r);return n;
}
function text(parent,value,role='Label',color='ink',w=null){const n=track(figma.createText());parent.appendChild(n);n.name=role+' / '+value.slice(0,24);n.fontName=['Title','Section'].includes(role)?fonts[2]:role==='Label'||role==='Button'?fonts[1]:fonts[0];n.resize(w||parent.width-parent.paddingLeft-parent.paddingRight,24);n.textAutoResize='HEIGHT';n.characters=value;n.fills=[p(neutral[color])];styleJobs.push(n.setTextStyleIdAsync(role==='Button'?labelStyle.id:BASE.styles[role]));return n;}

const primitiveCollection=await figma.variables.getVariableCollectionByIdAsync(BASE.collections.primitives);
const newPrimitives={};function rgb(h){const a=h.slice(1);return{r:parseInt(a.slice(0,2),16)/255,g:parseInt(a.slice(2,4),16)/255,b:parseInt(a.slice(4,6),16)/255};}
function prim(hex){const key=hex.toLowerCase();if(newPrimitives[key])return newPrimitives[key];const c=rgb(hex);let v=vars.find(v=>v.variableCollectionId===primitiveCollection.id&&v.resolvedType==='COLOR'&&Object.values(v.valuesByMode).some(x=>x&&x.r!==undefined&&Math.abs(x.r-c.r)<1e-7&&Math.abs(x.g-c.g)<1e-7&&Math.abs(x.b-c.b)<1e-7));if(!v){v=figma.variables.createVariable('button-study/'+key.slice(1),primitiveCollection,'COLOR');v.setValueForMode(primitiveCollection.modes[0].modeId,c);v.scopes=[];v.setVariableCodeSyntax('WEB','var(--mnd-button-study-'+key.slice(1)+')');createdVariableIds.push(v.id);}newPrimitives[key]=v;return v;}
let colorCollection=(await figma.variables.getLocalVariableCollectionsAsync()).find(c=>c.name==='M&D Button study / Color');if(colorCollection)throw new Error('Button study color collection already exists; resume from ledger.');colorCollection=figma.variables.createVariableCollection('M&D Button study / Color');colorCollection.renameMode(colorCollection.modes[0].modeId,'Friendly');const modes={Friendly:colorCollection.modes[0].modeId,Minimal:colorCollection.addMode('Minimal')};
const C={};for(const role of Object.keys(PALETTES.Friendly)){const v=figma.variables.createVariable('button/'+role,colorCollection,'COLOR');v.scopes=role==='border'||role==='focus'?['STROKE_COLOR']:role.startsWith('on')||role==='quiet'?['TEXT_FILL','STROKE_COLOR','SHAPE_FILL']:['FRAME_FILL','SHAPE_FILL'];v.setVariableCodeSyntax('WEB','var(--mnd-button-study-'+role+')');for(const direction of ['Friendly','Minimal'])v.setValueForMode(modes[direction],{type:'VARIABLE_ALIAS',id:prim(PALETTES[direction][role]).id});createdVariableIds.push(v.id);C[role]=v;}
let labelStyle=(await figma.getLocalTextStylesAsync()).find(s=>s.name==='M&D Button study/Label');if(!labelStyle){labelStyle=figma.createTextStyle();labelStyle.name='M&D Button study/Label';labelStyle.fontName=fonts[1];labelStyle.fontSize=16;labelStyle.lineHeight={unit:'PIXELS',value:24};}
const samples=['Primary','Secondary','Quiet','Pressed','Loading','Disabled','Focus'];
const made=await createComponentWithVariants({name:'M&D Button study / Direction & Sample',description:'用户要求的01清爽亲切与02极简克制，独立按钮比较；未选型，不替换手机草稿。Pressed / Loading / Focus是静态外观，不执行请求。',variantAxes:{Direction:['Friendly','Minimal'],Sample:samples},baseProps:{width:216,height:48,layoutMode:'HORIZONTAL',itemSpacing:8},page});
track(made.componentSet);made.componentSet.x=assetsX;made.componentSet.y=120;
const labelKey=made.componentSet.addComponentProperty('Label','TEXT','申请领养');const mains={};
for(let i=0;i<made.variants.length;i++){
 const n=track(made.variants[i]),direction=i<7?'Friendly':'Minimal',sample=samples[i%7];mains[direction+'/'+sample]=n;fixed(n,sample==='Quiet'?144:216,48);n.primaryAxisSizingMode='FIXED';n.counterAxisSizingMode='FIXED';n.counterAxisAlignItems='CENTER';n.primaryAxisAlignItems=sample==='Quiet'?'MIN':'CENTER';
 for(const key of ['paddingLeft','paddingRight'])space(n,key,16);space(n,'itemSpacing',8);radius(n,direction==='Friendly'?16:8);n.setExplicitVariableModeForCollection(colorCollection.id,modes[direction]);n.setExplicitVariableModeForCollection(BASE.collections.color,MOBILE.modeId);
 let bg='primary',fg='onPrimary';if(sample==='Secondary'){bg='secondary';fg='onSecondary';}if(sample==='Quiet'){bg=null;fg='quiet';}if(sample==='Pressed')bg='pressed';if(sample==='Disabled'){bg='disabled';fg='onDisabled';}
 n.fills=bg?[p(C[bg])]:[];n.strokes=sample==='Secondary'?[p(C.border)]:[];n.strokeWeight=1;n.strokeAlign='INSIDE';n.clipsContent=false;
 if(sample==='Focus'){const ring=track(figma.createRectangle());n.appendChild(ring);ring.name='Focus outline / 2px + 3px offset';ring.layoutPositioning='ABSOLUTE';ring.resize(n.width+6,n.height+6);ring.x=-3;ring.y=-3;ring.fills=[];ring.strokes=[p(C.focus)];ring.strokeWeight=2;ring.strokeAlign='OUTSIDE';ring.cornerRadius=n.cornerRadius+3;ring.constraints={horizontal:'STRETCH',vertical:'STRETCH'};}
 const t=text(n,sample==='Loading'?'提交中…':sample==='Disabled'?'请先完善申请':sample==='Secondary'?'先聊一聊':sample==='Quiet'?'查看领养条件':'申请领养',sample==='Quiet'?'Label':'Button','ink',sample==='Quiet'?84:184);t.fills=[p(C[fg])];t.textAlignHorizontal=sample==='Quiet'?'LEFT':'CENTER';t.layoutSizingHorizontal='FILL';t.componentPropertyReferences={characters:labelKey};
 if(sample==='Quiet'){const main=await figma.getNodeByIdAsync(MOBILE.components['icon/arrow-right']);const arrow=track(main.createInstance());n.appendChild(arrow);arrow.name='Arrow / secondary entry';arrow.resize(20,20);walk(arrow,c=>{if(c.id!==arrow.id)createdNodeIds.push(c.id);if('fills'in c&&Array.isArray(c.fills))c.fills=[];if('strokes'in c&&Array.isArray(c.strokes))c.strokes=c.strokes.map(x=>x.type==='SOLID'?figma.variables.setBoundVariableForPaint(x,'color',C.quiet):x);});}
}
await Promise.all(styleJobs);styleJobs.length=0;
function button(parent,direction,sample,label,width){const n=track(mains[direction+'/'+sample].createInstance());parent.appendChild(n);n.name='Button / '+direction+' / '+sample;n.setProperties({[labelKey]:label});n.resize(width,48);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='FIXED';walk(n,c=>{if(c.id!==n.id)createdNodeIds.push(c.id);});return n;}

const board=box(page,'M&D / Button directions 01-02',1200,48,24,'background',16);board.x=left;board.y=top;board.placeholder=true;board.setExplicitVariableModeForCollection(BASE.collections.color,MOBILE.modeId);
text(board,'按钮，先选你喜欢的感觉。','Title');text(board,'01 清爽亲切 / 02 极简克制 · 同文案、同尺寸、同场景','Body','muted');
const cols=box(board,'Two directions',1104,0,32,null,null,'HORIZONTAL');
for(const[direction,index,title,description]of [['Friendly','01','清爽亲切','柔和珊瑚＋白字，16px 圆角。中等字重，轻松亲近。'],['Minimal','02','极简克制','近黑＋白字，8px 圆角。用轮廓、留白与主次建立秩序。']]){
 const col=box(cols,'Direction / '+direction,536,24,16,'surface',16);col.setExplicitVariableModeForCollection(colorCollection.id,modes[direction]);text(col,index+' / '+title,'Section');text(col,description,'Label','muted');
 for(const[sample,label,caption]of [['Primary','申请领养','主操作'],['Secondary','先聊一聊','次操作'],['Quiet','查看领养条件','文字入口']]){const r=box(col,'Hierarchy / '+sample,488,0,16,null,null,'HORIZONTAL');r.counterAxisAlignItems='CENTER';text(r,caption,'Label','muted',96);button(r,direction,sample,label,sample==='Quiet'?144:358);}
 text(col,'反馈与不可操作状态','Section');
 for(const pair of [[['Pressed','申请领养','按下时'],['Loading','提交中…','加载中（静态）']],[['Disabled','请先完善申请','不可操作'],['Focus','申请领养','键盘焦点（示意）']]]){const row=box(col,'States',488,0,16,null,null,'HORIZONTAL');for(const[sample,label,caption]of pair){const cell=box(row,'State / '+sample,236,0,8);text(cell,caption,'Caption','muted');button(cell,direction,sample,label,236);}}
 text(col,'放进同一个手机操作区','Section');const wrap=box(col,'Mobile context wrapper',488,0,0);wrap.counterAxisAlignItems='CENTER';const scene=box(wrap,'390px action context',390,16,16,'background',16);text(scene,'小满 · 领养申请','Strong');text(scene,'看完条件后，选择下一步。','Caption','muted');const actions=box(scene,'Action hierarchy',358,0,12,null,null,'HORIZONTAL');button(actions,direction,'Secondary','先聊一聊',132);button(actions,direction,'Primary','申请领养',214);button(scene,direction,'Quiet','返回详情',116);
}
text(board,'共同规则：每个操作区只突出一个主动作；“返回”不占主按钮；禁用与加载状态不重复提交。','Label');text(board,'本页只是按钮方向提案，不代表完整 App 配色。所有状态为静态样例，待你选型后再统一页面。','Caption','muted');await Promise.all(styleJobs);board.placeholder=false;
const counts={},fontsSeen=new Set(),imageNodes=[];walk(board,n=>{counts[n.type]=(counts[n.type]||0)+1;if(n.type==='TEXT')for(const s of n.getStyledTextSegments(['fontName']))fontsSeen.add(s.fontName.family);if('fills'in n&&Array.isArray(n.fills)&&n.fills.some(x=>x.type==='IMAGE'))imageNodes.push({id:n.id,name:n.name,type:n.type,width:n.width,height:n.height});});
return{createdNodeIds,mutatedNodeIds,createdVariableIds,boardId:board.id,pageId:page.id,componentSetId:made.componentSet.id,components:Object.fromEntries(Object.entries(mains).map(([k,n])=>[k,n.id])),labelProperty:labelKey,colorCollectionId:colorCollection.id,modes,variables:Object.fromEntries(Object.entries(C).map(([k,v])=>[k,v.id])),primitiveIds:Object.fromEntries(Object.entries(newPrimitives).map(([k,v])=>[k,v.id])),textStyleId:labelStyle.id,width:board.width,height:board.height,variantCount:made.variants.length,nodeTypeCounts:counts,fontFamilies:[...fontsSeen],imageNodes,previousScreensModified:false};
