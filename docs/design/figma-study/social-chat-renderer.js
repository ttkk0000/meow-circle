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
if(STATE.radius16)G['radius/16']=byId[STATE.radius16];G['radius/24']=byId['VariableID:104:314'];
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

function addLink(n,to,key){if(to)actions[key||n.id]={sourceId:n.id,to};return n;}
function row(parent,label,value,to){const n=instance(parent,'row',{Title:label,Value:value},358);return addLink(n,to);}
function renderBase(parent,item){
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

const page=await figma.getNodeByIdAsync("56:25");await figma.setCurrentPageAsync(page);
const removedNodeIds=[];
function changePhoto(n,kind){const target=n.type==="INSTANCE"&&n.name==="Instance / socialAvatar"?n:n.findOne(x=>x.type==="INSTANCE"&&x.name==="Instance / socialAvatar");if(target)target.fills=[{type:"IMAGE",imageHash:BASE.images[kind||"catOffer"],scaleMode:"FILL"}];}
function render(parent,item){
 const w=parent.width-parent.paddingLeft-parent.paddingRight;
 if(item.type==="switch"){const r=box(parent,"Community destinations",w,0,8,null,0,"HORIZONTAL");for(const[label,to]of item.entries){const b=box(r,"Tab / "+label,(w-8*(item.entries.length-1))/item.entries.length,12,0,item.active===label?"brandSoft":null,24);dim(b,b.width,48);b.primaryAxisAlignItems="CENTER";const t=tx(b,label,"Label",item.active===label?"brand":"muted",b.width-24);t.textAlignHorizontal="CENTER";addLink(b,to);}return r;}
 if(item.type==="quickPublish"){const b=box(parent,"Share a pet moment",w,12,12,"surface",16,"HORIZONTAL");b.counterAxisAlignItems="CENTER";const av=instance(b,"socialAvatar",{},40);dim(av,40,40);changePhoto(av,"cat");tx(b,"晒晒今天的小可爱…","Label","muted",w-112);icon(b,"camera","brand",24);return addLink(b,"composePost");}
 if(item.type==="author"){const n=instance(parent,"socialAuthor",{Name:item.name,Pet:item.pet},w);changePhoto(n,item.kind);return addLink(n,item.to);}
 if(item.type==="socialFeed"){const r=box(parent,"Cat-first community feed",w,0,12,null,0,"HORIZONTAL");const cols=[box(r,"Feed left",(w-12)/2,0,16),box(r,"Feed right",(w-12)/2,0,16)];
 for(let i=0;i<item.posts.length;i++){const d=item.posts[i],n=instance(cols[i%2],"socialFeedTile",{Title:d.title,Owner:d.owner,Comments:d.comments},(w-12)/2);const ph=n.findOne(x=>x.name==="Photo / social");ph.fills=[{type:"IMAGE",imageHash:BASE.images[d.kind],scaleMode:"FILL"}];addLink(ph,d.to);addLink(n.findOne(x=>x.name==="Post copy"),d.to);addLink(n.findOne(x=>x.name==="Open comments"),d.to==="post"?"comments":d.to);}
 return r;}
 if(item.type==="socialActions"){const r=box(parent,"Like and comment",w,0,12,"surface",16,"HORIZONTAL");const l=instance(r,item.liked?"likeOn":"likeOff",{},80);if(item.likeTo)addLink(l,item.likeTo);tx(r,item.likes+" 人喜欢","Caption","muted",94);const b=box(r,"Open all comments",160,12,8,null,24,"HORIZONTAL");dim(b,160,48);b.counterAxisAlignItems="CENTER";icon(b,"message-circle","muted",20);tx(b,item.comments+" 条评论","Label","muted",100);addLink(b,item.commentTo);return r;}
 if(item.type==="comment"){const n=instance(parent,"socialComment",{Name:item.name,Body:item.body,Meta:item.meta},w);changePhoto(n,item.kind);addLink(n.findOne(x=>x.name==="Reply"),item.to);addLink(n.findOne(x=>x.name==="Comment menu"),"commentActions");return n;}
 if(item.type==="conversation"){const n=instance(parent,"socialConversation",{Name:item.name,Preview:item.preview,Meta:item.meta,Unread:item.unread||"·"},w);changePhoto(n,item.kind);return addLink(n,item.to);}
 if(item.type==="bubble"){const row=box(parent,item.mine?"Message alignment / Mine":"Message alignment / Theirs",w,0,0,null,0,"HORIZONTAL");row.primaryAxisAlignItems=item.mine?"MAX":"MIN";const n=instance(row,item.mine?"bubbleMine":"bubbleTheirs",{Message:item.text},Math.min(300,w-32));n.layoutSizingVertical="HUG";if(item.status)tx(parent,item.status,"Caption",item.status.includes("失败")?"danger":"muted");return n;}
 return renderBase(parent,item);
}
for(const data of BATCH){
 const root=await figma.getNodeByIdAsync(STATE.roots[data.id]);
 if(root.name.startsWith("Social V3 /")&&root.children.length&&!root.placeholder){screenNodes[data.id]=root.id;continue;}
 const existingText=root.findAllWithCriteria({types:["TEXT"]});const fs=new Map();for(const t of existingText)for(const s of t.getStyledTextSegments(["fontName"]))fs.set(JSON.stringify(s.fontName),s.fontName);for(const f of fs.values())await figma.loadFontAsync(f);
 for(const child of [...root.children]){walk(child,n=>removedNodeIds.push(n.id));child.remove();}
 mutatedNodeIds.push(root.id);root.name="Social V3 / "+data.phase+" / "+data.id;root.layoutMode="VERTICAL";dim(root,390,844);root.itemSpacing=0;root.fills=[paint("background")];root.cornerRadius=28;root.clipsContent=true;setMode(root);
 const status=box(root,"Status / learning disclosure",390,0,0,"surface",0,"HORIZONTAL");dim(status,390,28);status.paddingLeft=20;status.paddingRight=20;status.counterAxisAlignItems="CENTER";tx(status,"9:41","Caption","ink",100);const label=tx(status,"学习示意 · 非真实数据","Caption","muted",250);label.textAlignHorizontal="RIGHT";
 const bar=box(root,"Toolbar",390,8,4,"surface",0,"HORIZONTAL");dim(bar,390,56);bar.counterAxisAlignItems="CENTER";
 if(data.tab){bar.paddingLeft=16;bar.paddingRight=16;bar.primaryAxisAlignItems="SPACE_BETWEEN";tx(bar,data.brand?"M&D":data.title,data.brand?"MobileBrand":"Section","ink",data.brand?130:222);if(data.brand)button(bar,"武汉","city","quiet",132);const add=box(bar,data.more?"Settings":"Publish shortcut",48,12,0,null,24);dim(add,48,48);icon(add,data.more?"sliders-horizontal":"circle-plus","brand",24);addLink(add,data.more||"publishType");}
 else {const back=box(bar,"Back",48,12,0,null,24);dim(back,48,48);icon(back,"chevron-left");addLink(back,"BACK");tx(bar,data.title,"Section","ink",262);if(data.more){const more=box(bar,"More",48,12,0,null,24);dim(more,48,48);icon(more,"ellipsis");addLink(more,data.more);}}
 const footerH=data.tab?84:data.footer?88:28;const viewport=box(root,"Scrollable viewport",390,0,0);dim(viewport,390,844-28-56-footerH);viewport.clipsContent=true;viewport.overflowDirection="VERTICAL";
 const body=box(viewport,"Task content",390,16,16);for(const item of data.items)render(body,item);
 if(data.tab&&data.footer)button(body,data.footer.label,data.footer.to,data.footer.kind||"primary",358);
 const footer=box(root,data.tab?"Persistent navigation":data.footer?.type==="composer"?"Message composer":"Primary action",390,data.tab?8:data.footer?16:0,data.tab?0:8,"surface");dim(footer,390,footerH);footer.paddingBottom=12;
 if(data.tab){const tabs=box(footer,"Four destinations plus publish action",374,0,0,null,0,"HORIZONTAL");
 for(const[to,label,ik]of [["home","社区","house"],["adopt","领养","heart-handshake"],["publishType","发布","circle-plus"],["messages","消息","message-circle"],["mine","我的","user-round"]]){
 const key=to===data.tab?"navActive":"nav";const n=instance(tabs,key,{Label:label},74.8);n.setProperties({[prop(mains[key],"Icon")]:mains["icon/"+ik].id});addLink(n,to);}}
 else if(data.footer?.type==="composer"){const f=data.footer;const n=instance(footer,f.muted?"composerMuted":f.ready?"composerReady":"composerEmpty",{Hint:f.hint},358);
 if(!f.muted){addLink(n.findOne(x=>x.name==="Composer input"),f.sendTo);const send=n.findOne(x=>x.type==="INSTANCE"&&x.name==="Instance / primary");if(send)addLink(send,f.sendTo);}
 }
 else if(data.footer)button(footer,data.footer.label,data.footer.to,data.footer.kind||"primary",358);
 const indicator=box(footer,"Home indicator",120,0,0,"ink",999);dim(indicator,120,4);footer.counterAxisAlignItems="CENTER";root.placeholder=false;screenNodes[data.id]=root.id;
}
await Promise.all(styleJobs);
const linked=[],unresolved=[],selfLinks=[];
for(const [key,a]of Object.entries(actions)){const n=await figma.getNodeByIdAsync(a.sourceId);const dest=STATE.roots[a.to];if(a.to==="BACK"){n.reactions=[{trigger:{type:"ON_CLICK"},actions:[{type:"BACK"}]}];linked.push(a);}
 else if(dest){let top=n;while(top.parent&&top.parent.type!=="PAGE")top=top.parent;if(top.id===dest){selfLinks.push(a);continue;}n.reactions=[{trigger:{type:"ON_CLICK"},actions:[{type:"NODE",destinationId:dest,navigation:"NAVIGATE",transition:null}]}];linked.push(a);}
 else unresolved.push(a);
}
return {createdNodeIds,mutatedNodeIds,removedNodeIds,screenNodes,actions:linked,unresolved,selfLinks,screenCount:BATCH.length,renderedCount:Object.keys(screenNodes).length};

