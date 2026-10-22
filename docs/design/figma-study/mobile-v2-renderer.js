// Caller supplies BASE, STATE, CONTENT, ICONS, PHASE and (for screens) GROUP.
// Ordinary learning Design only; no network, SMS, submissions or app code.
// Initial construction recipe. Current phone visuals also require the Friendly rollout ledger.
if(STATE.buttonDirectionApproval?.scope?.includes('59')&&['setup','components'].includes(PHASE))throw new Error('Phone library already unified; do not reset setup/components over the Friendly rollout. Inspect current ledgers.');
const createdNodeIds=[],mutatedNodeIds=[],styleJobs=[],actions={},screenNodes={};
const fonts=[{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'ExtraBold'}];
await Promise.all(fonts.map(f=>figma.loadFontAsync(f)));
const allVars=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(allVars.map(v=>[v.id,v]));
const V=Object.fromEntries(Object.entries(BASE.variables).map(([k,id])=>[k,byId[id]]));
const G=Object.fromEntries(Object.entries(BASE.geometry).map(([k,id])=>[k,byId[id]]));
if(STATE.extraVariables)for(const[k,id]of Object.entries(STATE.extraVariables))V[k]=byId[id];
if(STATE.radius16)G['radius/16']=byId[STATE.radius16];
const paint=k=>figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:0,g:0,b:0}},'color',V[k]);
const track=n=>(createdNodeIds.push(n.id),n);
function walk(n,fn){fn(n);if('children'in n)for(const c of n.children)walk(c,fn);}
function dim(n,w,h){n.resize(w,h);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='FIXED';return n;}
function spacing(n,k,v){const variable=G['space/'+v];if(variable)n.setBoundVariable(k,variable);else n[k]=v;}
function rad(n,r){if(!r){n.cornerRadius=0;return;}for(const k of ['topLeftRadius','topRightRadius','bottomLeftRadius','bottomRightRadius'])n.setBoundVariable(k,G['radius/'+r]);}
function box(parent,name,w,pad=0,gap=12,bg=null,r=0,dir='VERTICAL'){
 const n=track(figma.createAutoLayout(dir));parent.appendChild(n);n.name=name;n.resize(w,64);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';
 for(const k of ['paddingLeft','paddingRight','paddingTop','paddingBottom'])spacing(n,k,pad);spacing(n,'itemSpacing',gap);n.fills=bg?[paint(bg)]:[];rad(n,r);return n;
}
function tx(parent,text,role='Body',color='ink',width=null){
 const n=track(figma.createText());parent.appendChild(n);n.name=role+' / '+text.slice(0,24);n.fontName=role==='MobileBrand'?fonts[3]:['Title','Section','Strong'].includes(role)?fonts[2]:role==='Label'?fonts[1]:fonts[0];
 n.resize(width||parent.width-parent.paddingLeft-parent.paddingRight,26);n.textAutoResize='HEIGHT';n.characters=text;n.fills=[paint(color)];
 styleJobs.push(n.setTextStyleIdAsync((STATE.styles||{})[role]||BASE.styles[role]).then(()=>{n.textAutoResize='HEIGHT';}));return n;
}
const mains={};if(STATE.components)for(const[k,id]of Object.entries({...BASE.components,...STATE.components}))mains[k]=await figma.getNodeByIdAsync(id);
function propOwner(main){return main.parent&&main.parent.type==='COMPONENT_SET'?main.parent:main;}
function textProp(main,node,key){const owner=propOwner(main);const known=Object.keys(owner.componentPropertyDefinitions).find(k=>k.split('#')[0]===key);node.componentPropertyReferences={characters:known||owner.addComponentProperty(key,'TEXT',node.characters)};}
function prop(main,key){return Object.keys(propOwner(main).componentPropertyDefinitions).find(k=>k.split('#')[0]===key);}
function instance(parent,key,values={},w=null){const c=mains[key];if(!c)throw new Error('Missing component '+key);const n=track(c.createInstance());parent.appendChild(n);n.name='Instance / '+key;
 const p={};for(const[k,v]of Object.entries(values))p[prop(c,k)]=v;n.setProperties(p);if(w){n.resize(w,n.height);if(['field','fieldError','select','petRow','feedTile','row'].includes(key))n.layoutSizingVertical='HUG';}
 walk(n,x=>{if(x.id!==n.id)createdNodeIds.push(x.id);});return n;
}
function icon(parent,key,color='ink',size=24){const n=instance(parent,'icon/'+key,{},size);n.resize(size,size);walk(n,c=>{if('fills'in c&&Array.isArray(c.fills))c.fills=[];if('strokes'in c&&Array.isArray(c.strokes))c.strokes=c.strokes.map(p=>p.type==='SOLID'?figma.variables.setBoundVariableForPaint(p,'color',V[color]):p);});return n;}
function button(parent,label,to=null,kind='secondary',w=null,key=''){const n=instance(parent,kind,{Label:label},w||parent.width-parent.paddingLeft-parent.paddingRight);if(to)actions[key||label+'/'+n.id]={sourceId:n.id,to};return n;}
function photo(parent,kind='cat',w=358,h=220){const n=track(figma.createFrame());parent.appendChild(n);n.name='Photo / '+kind+' / 非真实档案';n.resize(w,h);n.fills=[{type:'IMAGE',imageHash:BASE.images[kind],scaleMode:'FILL'}];rad(n,16);return n;}
function notice(parent,title,body,tone='subtle',compact=false){const n=box(parent,'Notice / '+title,parent.width-parent.paddingLeft-parent.paddingRight,compact?12:16,8,tone,14);const fg=tone==='dangerSoft'?'danger':tone==='warningSoft'?'warning':tone==='brandSoft'?'brand':'ink';tx(n,title,'Label',fg);tx(n,body,'Caption',fg);return n;}
function setMode(n){n.setExplicitVariableModeForCollection(BASE.collections.color,STATE.modeId);}

if(PHASE==='setup'){
 const color=await figma.variables.getVariableCollectionByIdAsync(BASE.collections.color),prim=await figma.variables.getVariableCollectionByIdAsync(BASE.collections.primitives),geom=await figma.variables.getVariableCollectionByIdAsync(BASE.collections.geometry);
 const mode=color.modes.find(m=>m.name==='Mobile Light V2')||{modeId:color.addMode('Mobile Light V2')};const modeId=mode.modeId;
 const palette={background:'#F7F8FA',surface:'#FFFEFC',subtle:'#F0F2F5',ink:'#202329',muted:'#616771',brand:'#BB3C17',onBrand:'#FFFEFC',brandSoft:'#FFF0E7',success:'#176C49',successSoft:'#E8F4ED',warning:'#86540A',warningSoft:'#FFF5DE',danger:'#B42332',dangerSoft:'#FDEEF0',border:'#E0E3E8',controlBorder:'#838A94',accent:'#FF8A50',onAccent:'#2F1B13'};
 const primitiveIds={},createdVariableIds=[],extraVariables={};
 for(const[k,hex]of Object.entries(palette)){const name='mobile-v2/'+k;let v=allVars.find(v=>v.name===name&&v.variableCollectionId===prim.id);if(!v){v=figma.variables.createVariable(name,prim,'COLOR');const c=hex.slice(1);v.setValueForMode(prim.modes[0].modeId,{r:parseInt(c.slice(0,2),16)/255,g:parseInt(c.slice(2,4),16)/255,b:parseInt(c.slice(4,6),16)/255});v.scopes=[];v.setVariableCodeSyntax('WEB','var(--mnd-mobile-v2-'+k+')');createdVariableIds.push(v.id);}primitiveIds[k]=v.id;
  let semantic=V[k];if(!semantic){semantic=figma.variables.createVariable('color/'+k,color,'COLOR');semantic.scopes=['FRAME_FILL','SHAPE_FILL','TEXT_FILL','STROKE_COLOR'];semantic.setVariableCodeSyntax('WEB','var(--mnd-mobile-v2-'+k+')');for(const m of color.modes)semantic.setValueForMode(m.modeId,{type:'VARIABLE_ALIAS',id:v.id});createdVariableIds.push(semantic.id);extraVariables[k]=semantic.id;}
  semantic.setValueForMode(modeId,{type:'VARIABLE_ALIAS',id:v.id});
 }
 let radius16=allVars.find(v=>v.name==='radius/16'&&v.variableCollectionId===geom.id);if(!radius16){radius16=figma.variables.createVariable('radius/16',geom,'FLOAT');radius16.setValueForMode(geom.modes[0].modeId,16);radius16.scopes=['CORNER_RADIUS'];radius16.setVariableCodeSyntax('WEB','var(--mnd-mobile-v2-radius-card)');createdVariableIds.push(radius16.id);}
 const styles={};for(const[role,size,line,font]of [['MobileBrand',24,32,fonts[3]],['Strong',16,24,fonts[2]]]){let s=(await figma.getLocalTextStylesAsync()).find(s=>s.name==='M&D Mobile V2/'+role);if(!s){s=figma.createTextStyle();s.name='M&D Mobile V2/'+role;s.fontName=font;s.fontSize=size;s.lineHeight={unit:'PIXELS',value:line};}styles[role]=s.id;}
 const pages={};for(const[key,name]of [['phone','10 手机主版本 V2 · 浅色'],['guide','11 手机 V2 · 视觉与交接']]){let p=figma.root.children.find(p=>p.name===name);if(!p){p=track(figma.createPage());p.name=name;}pages[key]=p.id;}
 const roots={};const p=await figma.getNodeByIdAsync(pages.phone);await figma.setCurrentPageAsync(p);
 for(let i=0;i<CONTENT.length;i++){const data=CONTENT[i];const name='Mobile V2 / '+String(i+1).padStart(2,'0')+' / '+data.group+' / '+data.id;let root=p.children.find(n=>n.name===name);if(!root){root=box(p,name,390,0,0,'background',28);dim(root,390,844);root.setExplicitVariableModeForCollection(BASE.collections.color,modeId);root.clipsContent=true;root.x=160+i%7*438;root.y=220+Math.floor(i/7)*988;root.placeholder=true;}roots[data.id]=root.id;}
 return{createdNodeIds,mutatedNodeIds,pages,roots,modeId,palette,primitiveIds,extraVariables,createdVariableIds,radius16:radius16.id,styles,screenCount:CONTENT.length};
}

const page=await figma.getNodeByIdAsync(STATE.pages[PHASE==='components'||PHASE==='guide'?'guide':'phone']);await figma.setCurrentPageAsync(page);
if(PHASE==='components'){
 if(page.children.find(n=>n.name==='Mobile V2 / Construction'))throw new Error('Components already exist; resume from ledger');
 const area=box(page,'Mobile V2 / Construction',760,24,24,null);area.x=2000;area.y=120;setMode(area);const output={};
 function main(key,name,w=358){const n=track(figma.createComponent());area.appendChild(n);n.name='M&D Mobile V2 / '+name;n.layoutMode='VERTICAL';n.resize(w,64);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.fills=[];spacing(n,'itemSpacing',8);setMode(n);n.description='M&D 非商业手机 V2；局部可复用组件，学习配色与生产工程隔离。';output[key]=n;mains[key]=n;return n;}
 for(const[key,svg]of Object.entries(ICONS)){const n=main('icon/'+key,'Icon / '+key,24);dim(n,24,24);const svgNode=track(figma.createNodeFromSvg(svg.replace(/currentColor/g,'#202329')));n.appendChild(svgNode);svgNode.name='Lucide / '+key;svgNode.resize(24,24);walk(svgNode,c=>{if(c.id!==svgNode.id)createdNodeIds.push(c.id);if('strokes'in c&&Array.isArray(c.strokes))c.strokes=c.strokes.map(p=>p.type==='SOLID'?paint('ink'):p);});}
 const made=await createComponentWithVariants({name:'M&D Mobile V2 / Action',description:'Style variants; disabled has no submission behavior. Scoped mobile learning components.',variantAxes:{Style:['Primary','Secondary','Quiet','Disabled']},baseProps:{width:358,height:48,layoutMode:'HORIZONTAL',itemSpacing:8},page});
 createdNodeIds.push(made.componentSet.id);made.componentSet.x=2850;made.componentSet.y=120;
 for(let i=0;i<made.variants.length;i++){const c=made.variants[i];createdNodeIds.push(c.id);const key=['primary','secondary','quiet','disabled'][i];const bg=['accent','subtle',null,'subtle'][i],fg=['onAccent','ink','brand','muted'][i];c.fills=bg?[paint(bg)]:[];rad(c,14);for(const k of ['paddingLeft','paddingRight'])spacing(c,k,16);const t=tx(c,'主要操作',key==='quiet'?'Label':'Strong',fg,326);t.textAlignHorizontal='CENTER';t.layoutSizingHorizontal='FILL';textProp(c,t,'Label');output[key]=c;mains[key]=c;setMode(c);}
 for(const key of ['field','fieldError','select']){const c=main(key,'Field / '+key);textProp(c,tx(c,'字段标签','Label'),'Label');const row=box(c,'Input control',358,12,8,'surface',14,'HORIZONTAL');row.minHeight=48;row.strokes=[paint(key==='fieldError'?'danger':'controlBorder')];row.strokeWeight=1;row.counterAxisAlignItems='CENTER';textProp(c,tx(row,'请选择或填写','Body','ink',key==='select'?298:334),'Value');if(key==='select')icon(row,'chevron-right','muted',20);}
 let c=main('petRow','Pet row');c.layoutMode='HORIZONTAL';c.fills=[paint('surface')];rad(c,16);for(const k of ['paddingLeft','paddingRight','paddingTop','paddingBottom'])spacing(c,k,12);spacing(c,'itemSpacing',12);photo(c,'cat',96,112).name='Photo / pet';const petBody=box(c,'Pet facts',226,0,8);textProp(c,tx(petBody,'小满','Strong'),'Title');textProp(c,tx(petBody,'武汉 · 猫咪 · 8 个月','Caption','muted'),'Meta');textProp(c,tx(petBody,'领养 · 关系和资料可展开','Caption','brand'),'Note');petBody.layoutSizingHorizontal='FILL';
 c=main('feedTile','Feed tile',173);photo(c,'cat',173,198).name='Photo / feed';const tileBody=box(c,'Tile copy',173,0,4);textProp(c,tx(tileBody,'迎接第一只猫，先准备好','Strong'),'Title');textProp(c,tx(tileBody,'编辑部 · 学习示例','Caption','muted'),'Meta');tileBody.layoutSizingHorizontal='FILL';
 c=main('row','List row');c.layoutMode='HORIZONTAL';c.minHeight=64;spacing(c,'itemSpacing',8);c.counterAxisAlignItems='CENTER';const copy=box(c,'Row copy',326,0,4);textProp(c,tx(copy,'列表名称','Body'),'Title');textProp(c,tx(copy,'当前值或下一步','Caption','muted'),'Value');copy.layoutSizingHorizontal='FILL';icon(c,'chevron-right','muted',20);
 for(const key of ['nav','navActive']){c=main(key,'Navigation / '+key,71.6);dim(c,71.6,56);c.counterAxisAlignItems='CENTER';spacing(c,'itemSpacing',0);const ni=icon(c,'house',key==='navActive'?'brand':'muted',24);ni.name='Navigation icon';c.addComponentProperty('Icon','INSTANCE_SWAP',mains['icon/house'].id);ni.componentPropertyReferences={mainComponent:Object.keys(c.componentPropertyDefinitions).find(k=>k.startsWith('Icon#'))};const title=tx(c,'社区','Caption',key==='navActive'?'brand':'muted',71.6);title.textAlignHorizontal='CENTER';textProp(c,title,'Label');}
 for(const c of Object.values(output)){walk(c,n=>{if(n.type==='TEXT'){n.textAutoResize='HEIGHT';if(n.parent&&n.parent.layoutMode!=='NONE')n.layoutSizingHorizontal='FILL';}});}
 await Promise.all(styleJobs);return{createdNodeIds,mutatedNodeIds,components:Object.fromEntries(Object.entries(output).map(([k,n])=>[k,n.id])),actionSetId:made.componentSet.id,componentCount:Object.keys(output).length};
}

function addLink(n,to,key){if(to)actions[key||n.id]={sourceId:n.id,to};return n;}
function row(parent,label,value,to){const n=instance(parent,'row',{Title:label,Value:value},358);return addLink(n,to);}
function render(parent,item){
 const w=parent.width-parent.paddingLeft-parent.paddingRight;
 if(item.type==='text')return tx(parent,item.text,item.role||'Body',item.color||'ink');
 if(item.type==='photo'){photo(parent,item.kind,w,item.height);return tx(parent,'学习素材 · 非真实档案','Caption','muted');}
 if(item.type==='note')return notice(parent,item.title,item.body,item.tone,item.compact);
 if(item.type==='step')return tx(parent,item.text,'Label','brand');
 if(item.type==='action')return button(parent,item.label,item.to,item.kind);
 if(item.type==='row')return row(parent,item.label,item.value,item.to);
 if(item.type==='field'){const n=instance(parent,item.kind==='select'?'select':item.kind==='error'?'fieldError':'field',{Label:item.label,Value:item.value},w);if(item.hint)tx(parent,item.hint,'Caption',item.kind==='error'?'danger':'muted');return n;}
 if(item.type==='pet'){const n=instance(parent,'petRow',{Title:item.name,Meta:item.meta,Note:item.note},w);const p=n.findOne(n=>n.name==='Photo / pet');p.fills=[{type:'IMAGE',imageHash:BASE.images[item.kind||'cat'],scaleMode:'FILL'}];return addLink(n,item.to);}
 if(item.type==='search'){const n=box(parent,'Search',w,12,8,'surface',14,'HORIZONTAL');n.minHeight=48;n.counterAxisAlignItems='CENTER';icon(n,'search','muted',20);tx(n,item.label,'Label','muted',w-52);return addLink(n,item.to);}
 if(item.type==='chips'){const n=box(parent,'Filters',w,0,8,null,0,'HORIZONTAL');item.labels.forEach((label,i)=>{const c=instance(n,i===(item.active||0)?'chipSelected':'chip',{Label:label},label.length*14+28);dim(c,label.length*14+28,40);});return n;}
 if(item.type==='inlineActions'){const n=box(parent,'Context actions',w,0,12,null,0,'HORIZONTAL');for(const[label,to]of item.entries)button(n,label,to,'quiet',(w-12*(item.entries.length-1))/item.entries.length);return n;}
 if(item.type==='shortcuts'){const n=box(parent,'Community shortcuts',w,0,12,null,0,'HORIZONTAL');for(const[label,ik,to]of item.entries){const c=box(n,'Shortcut / '+label,(w-24)/3,8,4,'surface',14);c.counterAxisAlignItems='CENTER';icon(c,ik,'brand',24);const t=tx(c,label,'Label','ink',c.width-16);t.textAlignHorizontal='CENTER';addLink(c,to);}return n;}
 if(item.type==='feed'){const n=box(parent,'Photo-led community',w,0,12,null,0,'HORIZONTAL');const cols=[box(n,'Feed left',(w-12)/2,0,16),box(n,'Feed right',(w-12)/2,0,16)];item.entries.forEach(([title,meta,kind,to],i)=>{const card=instance(cols[i%2],'feedTile',{Title:title,Meta:meta},(w-12)/2);card.findOne(n=>n.name==='Photo / feed').fills=[{type:'IMAGE',imageHash:BASE.images[kind],scaleMode:'FILL'}];addLink(card,to);});return n;}
 if(item.type==='facts'){const n=box(parent,'Comparable facts',w,0,12);for(const[label,value]of item.entries){const r=box(n,'Fact / '+label,w,0,8,null,0,'HORIZONTAL');tx(r,label,'Label','muted',84);tx(r,value,'Body','ink',w-92);}return n;}
 if(item.type==='empty'){const n=box(parent,'Honest empty state',w,24,12,'surface',16);n.counterAxisAlignItems='CENTER';icon(n,'paw-print','brand',48);const title=tx(n,item.title,'Section','ink',w-48);title.textAlignHorizontal='CENTER';const body=tx(n,item.body,'Label','muted',w-48);body.textAlignHorizontal='CENTER';return n;}
 if(item.type==='upload'){const n=box(parent,'Upload / '+item.label,w,16,8,'surface',14);n.strokes=[paint('controlBorder')];n.strokeWeight=1;icon(n,'camera','muted',24);tx(n,item.label,'Label');tx(n,item.hint,'Caption','muted');return n;}
 if(item.type==='timeline'){const n=box(parent,'Status / distinct stages',w,0,16);item.entries.forEach(([title,body],i)=>{const r=box(n,'Stage '+i,w,0,12,null,0,'HORIZONTAL');const step=box(r,'Step '+(i+1),32,4,0,i===0?'brandSoft':'subtle',999);dim(step,32,32);const number=tx(step,String(i+1),'Label',i===0?'brand':'muted',24);number.textAlignHorizontal='CENTER';const copy=box(r,'Stage content',w-44,0,4);tx(copy,title,'Strong');tx(copy,body,'Caption','muted');});return n;}
 if(item.type==='bubble'){const n=box(parent,item.mine?'Message / me':'Message / them',w,16,0,item.mine?'brandSoft':'surface',16);return tx(n,item.text,'Body',item.mine?'brand':'ink');}
 if(item.type==='consent'){const n=box(parent,'Consent / unchecked',w,0,12,null,0,'HORIZONTAL');const checkbox=box(n,'Checkbox / unchecked',24,0,0,'surface',8);dim(checkbox,24,24);checkbox.strokes=[paint('controlBorder')];checkbox.strokeWeight=1;tx(n,item.text,'Caption','muted',w-36);return n;}
 if(item.type==='profile'){const n=box(parent,'Profile identity',w,16,12,'surface',16,'HORIZONTAL');const img=photo(n,'cat',64,64);rad(img,999);const c=box(n,'Identity copy',w-108,0,4);tx(c,item.name,'Section');tx(c,item.subtitle,'Caption','muted');return n;}
 throw new Error('Unknown block '+item.type);
}
if(PHASE==='screens'){
 const batch=CONTENT.filter(s=>s.group===GROUP);
 for(const data of batch){const root=await figma.getNodeByIdAsync(STATE.roots[data.id]);if(root.children.length){screenNodes[data.id]=root.id;continue;}mutatedNodeIds.push(root.id);setMode(root);
  const status=box(root,'Status / study label',390,0,0,'surface',0,'HORIZONTAL');dim(status,390,28);spacing(status,'paddingLeft',20);spacing(status,'paddingRight',20);status.counterAxisAlignItems='CENTER';tx(status,'9:41','Caption','ink',100);const label=tx(status,'学习示意 · 非真实数据','Caption','muted',250);label.textAlignHorizontal='RIGHT';
  const bar=box(root,'Toolbar',390,8,4,'surface',0,'HORIZONTAL');dim(bar,390,56);bar.counterAxisAlignItems='CENTER';
  if(data.tab){spacing(bar,'paddingLeft',16);spacing(bar,'paddingRight',16);bar.primaryAxisAlignItems='SPACE_BETWEEN';tx(bar,data.id==='home'?'M&D':data.title,data.id==='home'?'MobileBrand':'Section','ink',data.id==='home'?130:222);if(data.id==='home'){button(bar,'武汉','city','quiet',132);}const add=box(bar,'Publish shortcut',48,12,0,null,14);dim(add,48,48);icon(add,'circle-plus','brand',24);addLink(add,'publishType');}
  else{const back=box(bar,'Back',48,12,0,null,14);dim(back,48,48);icon(back,'chevron-left');actions[data.id+'/back']={sourceId:back.id,to:'BACK'};tx(bar,data.title,'Section','ink',262);const more=box(bar,'More / report',48,12,0,null,14);dim(more,48,48);icon(more,'ellipsis');addLink(more,'report');}
  const footerH=data.tab?84:88;const viewport=box(root,'Scrollable viewport',390,0,0);dim(viewport,390,844-28-56-footerH);viewport.clipsContent=true;viewport.overflowDirection='VERTICAL';const body=box(viewport,'Task content',390,16,16);for(const item of data.items)render(body,item);
  const footer=box(root,data.tab?'Persistent navigation':'Primary action',390,data.tab?8:16,data.tab?0:8,'surface');dim(footer,390,footerH);spacing(footer,'paddingBottom',12);
  if(data.tab){const tabs=box(footer,'Four destinations plus publish action',374,0,0,null,0,'HORIZONTAL');const navs=[['home','社区','house'],['adopt','领养','heart-handshake'],['publishType','发布','circle-plus'],['messages','消息','message-circle'],['mine','我的','user-round']];for(const[to,label,ik]of navs){const c=instance(tabs,to===data.tab?'navActive':'nav',{Label:label},74.8);const swap=Object.keys(mains[to===data.tab?'navActive':'nav'].componentPropertyDefinitions).find(k=>k.startsWith('Icon#'));c.setProperties({[swap]:mains['icon/'+ik].id});addLink(c,to,data.id+'/nav/'+to);}}
  else if(data.footer){if(data.footer.secondary){const actionsRow=box(footer,'Action hierarchy / Friendly rollout',358,0,12,null,0,'HORIZONTAL');button(actionsRow,data.footer.secondary.label,data.footer.secondary.to,'secondary',132,data.id+'/secondary');button(actionsRow,data.footer.label,data.footer.to,'primary',214,data.id+'/primary');}else button(footer,data.footer.label,data.footer.to,'primary',358,data.id+'/primary');}
  const indicator=box(footer,'Home indicator',120,0,0,'ink',999);dim(indicator,120,4);footer.counterAxisAlignItems='CENTER';root.placeholder=false;screenNodes[data.id]=root.id;
 }
 await Promise.all(styleJobs);return{createdNodeIds,mutatedNodeIds,screenNodes,actions,group:GROUP,count:batch.length};
}
if(PHASE==='guide'){
 const review=box(page,'Mobile V2 / 视觉与交接',1344,48,24,'background',16);review.x=160;review.y=120;setMode(review);tx(review,'M&D / meow & doggie','MobileBrand');tx(review,'先把手机，做成真正顺手的日常。','Title');tx(review,'浅色手机主版本 · 59 屏学习提案 · Pad / 桌面 / Web 暂缓','Label','muted');
 const row=box(review,'Three representative native snapshots',1248,0,24,null,0,'HORIZONTAL');const cloneIds=[];for(const id of ['home','adopt','detail']){const original=await figma.getNodeByIdAsync(STATE.roots[id]);const c=track(original.clone());row.appendChild(c);c.name='Review snapshot / '+id;setMode(c);walk(c,n=>{if(n.id!==c.id)createdNodeIds.push(n.id);if('reactions'in n&&n.reactions.length)n.reactions=[];});cloneIds.push(c.id);}
 notice(review,'不是“大陆人统一喜欢橙色”','依据近期养宠资料与用户反馈，采用亲近的照片流、清楚的中文任务与少量杏橙动作。具体审美仍需武汉／上海目标用户走查。');
 tx(review,'社区是照片流；领养是事实比较；表单是左对齐的任务步骤。','Section');tx(review,'新增学习配色模式保留旧 Light / Dark。来源、核验、费用和报告边界继续保留；普通 Design 不是原生 Spec。','Body','muted');tx(review,'底部四个目的地＋发布快捷动作；出售／有偿转让退至领养辅助入口。此为本轮手机导航提案，未迁移工程。','Label');
 tx(review,'未接入真实地图、短信、发布、举报或付款；原型只切换静态状态。未执行设备、文字放大、键盘／读屏或完整 WCAG 验收。','Caption','muted');
 await Promise.all(styleJobs);return{createdNodeIds,mutatedNodeIds,reviewId:review.id,cloneIds,width:review.width,height:review.height};
}
throw new Error('Unknown phase '+PHASE);
