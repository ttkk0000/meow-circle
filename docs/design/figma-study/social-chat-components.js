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


const page=await figma.getNodeByIdAsync("56:26");await figma.setCurrentPageAsync(page);
if(page.children.some(n=>n.name==="Social V3 / Components"))throw new Error("Social components already exist; resume ledger.");
const area=box(page,"Social V3 / Components",760,24,24,null);area.x=6600;area.y=200;setMode(area);
const output={},sets={};
function main(key,w=358,dir="VERTICAL"){
 const n=track(figma.createComponent());area.appendChild(n);n.name="M&D Mobile V3 / "+key;n.layoutMode=dir;dim(n,w,64);n.layoutSizingVertical="HUG";n.fills=[];spacing(n,"itemSpacing",8);setMode(n);
 n.description="手机学习稿 Friendly 社交组件；复用字体、语义颜色与照片。交互为设计演示，不连接真实服务。";mains[key]=n;output[key]=n;return n;
}
function expose(c,node,key){textProp(c,node,key);}
let c=main("socialAvatar",48);dim(c,48,48);c.fills=[{type:"IMAGE",imageHash:BASE.images.catOffer,scaleMode:"FILL"}];rad(c,999);
c=main("socialAuthor",358,"HORIZONTAL");c.counterAxisAlignItems="CENTER";spacing(c,"itemSpacing",12);instance(c,"socialAvatar");let b=box(c,"Author copy",298,0,0);expose(c,tx(b,"布丁的铲屎官","Strong"),"Name");expose(c,tx(b,"布丁 · 猫咪","Caption","muted"),"Pet");
const likeSet=await createComponentWithVariants({name:"M&D Mobile V3 / Like",description:"48px 触控点赞控件；点按切换默认／已点赞。数字仅为示例，不模拟全局计数同步。",variantAxes:{State:["Off","On"]},baseProps:{width:80,height:48,layoutMode:"HORIZONTAL",itemSpacing:4},page});
createdNodeIds.push(likeSet.componentSet.id);likeSet.componentSet.x=7450;likeSet.componentSet.y=200;sets.like=likeSet.componentSet.id;
for(let i=0;i<2;i++){c=likeSet.variants[i];createdNodeIds.push(c.id);setMode(c);rad(c,24);c.fills=i?[paint("brandSoft")]:[];spacing(c,"itemSpacing",4);icon(c,"heart",i?"brand":"muted",20);tx(c,i?"已赞":"点赞","Label",i?"brand":"muted",42);mains[i?"likeOn":"likeOff"]=c;output[i?"likeOn":"likeOff"]=c;}
for(let i=0;i<2;i++)likeSet.variants[i].reactions=[{trigger:{type:"ON_CLICK"},actions:[{type:"NODE",destinationId:likeSet.variants[1-i].id,navigation:"CHANGE_TO",transition:null}]}];
c=main("socialFeedTile",173);c.fills=[paint("surface")];rad(c,16);c.clipsContent=true;spacing(c,"itemSpacing",0);photo(c,"catOffer",173,190).name="Photo / social";
b=box(c,"Post copy",173,8,4);expose(c,tx(b,"太阳晒到肚皮啦","Strong"),"Title");expose(c,tx(b,"布丁的铲屎官","Caption","muted"),"Owner");
const metrics=box(c,"Feed engagement",173,4,0,null,0,"HORIZONTAL");instance(metrics,"likeOff",{},80);const comments=box(metrics,"Open comments",85,4,4,null,24,"HORIZONTAL");dim(comments,85,48);comments.counterAxisAlignItems="CENTER";icon(comments,"message-circle","muted",20);expose(c,tx(comments,"3","Label","muted",42),"Comments");
c=main("socialComment");spacing(c,"itemSpacing",8);c.fills=[paint("surface")];rad(c,16);for(const k of ["paddingLeft","paddingRight","paddingTop","paddingBottom"])spacing(c,k,12);
const head=box(c,"Comment identity",334,0,8,null,0,"HORIZONTAL");const avatar=instance(head,"socialAvatar",{},32);dim(avatar,32,32);expose(c,tx(head,"团子今天不拆家","Label","ink",294),"Name");
expose(c,tx(c,"这个姿势也太可爱了！","Body","ink",334),"Body");
const tools=box(c,"Comment actions",334,0,4,null,0,"HORIZONTAL");const reply=box(tools,"Reply",198,0,0,null,0);dim(reply,198,48);reply.primaryAxisAlignItems="CENTER";expose(c,tx(reply,"2 赞 · 查看回复","Caption","muted",198),"Meta");instance(tools,"likeOff",{},80);
const more=box(tools,"Comment menu",48,12,0,null,24);dim(more,48,48);icon(more,"ellipsis","muted",24);
c=main("socialConversation",358,"HORIZONTAL");for(const k of ["paddingTop","paddingBottom"])spacing(c,k,8);spacing(c,"itemSpacing",12);c.counterAxisAlignItems="CENTER";instance(c,"socialAvatar");
b=box(c,"Conversation copy",250,0,0);expose(c,tx(b,"布丁的铲屎官","Strong","ink",250),"Name");expose(c,tx(b,"原来你家也爱晒太阳呀。","Label","muted",250),"Preview");expose(c,tx(b,"养宠交流 · 学习示例","Caption","muted",250),"Meta");
const badge=box(c,"Unread badge",36,4,0,"brandSoft",999);dim(badge,36,32);badge.primaryAxisAlignItems="CENTER";expose(c,tx(badge,"2","Caption","brand",28),"Unread");
const bubbles=await createComponentWithVariants({name:"M&D Mobile V3 / Chat bubble",description:"普通交流的我方／对方气泡；消息状态另行展示，不能替代领养关键确认。",variantAxes:{Side:["Theirs","Mine"]},baseProps:{width:300,height:76,layoutMode:"VERTICAL",padding:{top:12,bottom:12,left:16,right:16},itemSpacing:4},page});
createdNodeIds.push(bubbles.componentSet.id);bubbles.componentSet.x=7450;bubbles.componentSet.y=420;sets.bubble=bubbles.componentSet.id;
for(let i=0;i<2;i++){c=bubbles.variants[i];createdNodeIds.push(c.id);c.primaryAxisAlignItems="MIN";c.counterAxisAlignItems="MIN";c.layoutSizingVertical="HUG";setMode(c);rad(c,16);c.fills=[paint(i?"brandSoft":"surface")];for(const k of ["paddingTop","paddingBottom"])spacing(c,k,12);for(const k of ["paddingLeft","paddingRight"])spacing(c,k,16);expose(c,tx(c,"今天又被小家伙治愈啦。","Body",i?"brand":"ink",268),"Message");output[i?"bubbleMine":"bubbleTheirs"]=c;mains[i?"bubbleMine":"bubbleTheirs"]=c;}
const composers=await createComponentWithVariants({name:"M&D Mobile V3 / Message composer",description:"静态输入与发送示例。输入容器16px圆角；发送按钮48高24圆角。禁言状态不开放输入／发送。",variantAxes:{State:["Empty","Ready","Muted"]},baseProps:{width:358,height:48,layoutMode:"HORIZONTAL",itemSpacing:12},page});
createdNodeIds.push(composers.componentSet.id);composers.componentSet.x=7450;composers.componentSet.y=680;sets.composer=composers.componentSet.id;
for(let i=0;i<3;i++){c=composers.variants[i];createdNodeIds.push(c.id);setMode(c);c.fills=[];c.counterAxisAlignItems="CENTER";spacing(c,"itemSpacing",12);
const input=box(c,"Composer input",282,12,0,"subtle",16);dim(input,282,48);input.primaryAxisAlignItems="CENTER";expose(c,tx(input,i===2?"禁言中，暂不可发送":i===1?"小满也爱晒太阳！":"聊聊你的想法…","Label","muted",258),"Hint");
const send=instance(c,i===2?"disabled":"primary",{Label:i===2?"禁言":"发送"},64);dim(send,64,48);output[["composerEmpty","composerReady","composerMuted"][i]]=c;mains[["composerEmpty","composerReady","composerMuted"][i]]=c;}
await Promise.all(styleJobs);
return {createdNodeIds,mutatedNodeIds,areaId:area.id,components:Object.fromEntries(Object.entries(output).map(([k,n])=>[k,n.id])),sets,count:Object.keys(output).length};
