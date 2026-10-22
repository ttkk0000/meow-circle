// Figma Design recipe only. Caller supplies CONFIG, BASE, MOBILE, STATE and PLATFORM.
// Reuses existing phone components and variables; never run as application code.
const page = await figma.getNodeByIdAsync(CONFIG.pages[PLATFORM]);
await figma.setCurrentPageAsync(page);
const createdNodeIds = [], mutatedNodeIds = [], removedNodeIds = [], roots = {}, links = [], jobs = [];
const fonts = [{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'},{family:'Manrope',style:'ExtraBold'}];
await Promise.all(fonts.map(f=>figma.loadFontAsync(f)));
const allVars = await figma.variables.getLocalVariablesAsync();
const byId = Object.fromEntries(allVars.map(v=>[v.id,v]));
const V = Object.fromEntries(Object.entries(BASE.variables).map(([k,id])=>[k,byId[id]]));
V.accent=byId['VariableID:56:19']; V.onAccent=byId['VariableID:56:21'];
const geo=Object.fromEntries(allVars.filter(v=>v.variableCollectionId===BASE.collections.geometry).map(v=>[v.name,v]));
const styles={...BASE.styles,...MOBILE.styles};
const comps={}; await Promise.all(Object.entries({...MOBILE.components,...STATE.components}).map(async([k,id])=>{comps[k]=await figma.getNodeByIdAsync(id);}));
function cv(v){const x=v.valuesByMode[CONFIG.modeId]??Object.values(v.valuesByMode)[0];return x?.type==='VARIABLE_ALIAS'?cv(byId[x.id]):x;}
function paint(key){const v=V[key],c=cv(v);return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:c.r,g:c.g,b:c.b}},'color',v);}
const track=n=>(createdNodeIds.push(n.id),n);
function dim(n,w,h){n.resize(w,h);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='FIXED';return n;}
function bindGeo(n,key,value){const v=geo[key+'/'+value];if(!v)return;if(key==='radius'){for(const p of ['topLeftRadius','topRightRadius','bottomLeftRadius','bottomRightRadius'])n.setBoundVariable(p,v);}else n.setBoundVariable(key,v);}
function box(p,name,w,pad=0,gap=16,bg=null,r=0,dir='VERTICAL'){
  const n=track(figma.createAutoLayout(dir));p.appendChild(n);n.name=name;n.resize(w,64);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';
  n.paddingLeft=n.paddingRight=n.paddingTop=n.paddingBottom=pad;n.itemSpacing=gap;n.fills=bg?[paint(bg)]:[];n.cornerRadius=r;
  for(const k of ['paddingLeft','paddingRight','paddingTop','paddingBottom'])if(geo['space/'+pad])n.setBoundVariable(k,geo['space/'+pad]);
  if(geo['space/'+gap])n.setBoundVariable('itemSpacing',geo['space/'+gap]);bindGeo(n,'radius',r);return n;
}
function text(p,s,role='Body',color='ink',w=null){
  const n=track(figma.createText());p.appendChild(n);n.name=role+' / '+s.slice(0,36);n.fontName=role==='MobileBrand'?fonts[3]:['Title','Section','Strong'].includes(role)?fonts[2]:role==='Label'?fonts[1]:fonts[0];
  n.characters=s;n.fontSize={MobileBrand:24,Title:28,Section:20,Strong:16,Body:16,Label:14,Caption:12}[role]||16;n.lineHeight={unit:'PERCENT',value:150};n.letterSpacing={unit:'PIXELS',value:0};n.resize(w??(p.width-p.paddingLeft-p.paddingRight),24);n.textAutoResize='HEIGHT';n.fills=[paint(color)];
  if(styles[role])jobs.push(n.setTextStyleIdAsync(styles[role]));return n;
}
function click(n,to){if(to){links.push({id:n.id,to});n.name=n.name+' → '+to;}return n;}
function inst(p,k,props={},w=null){
  const c=comps[k];if(!c)throw Error('Missing component '+k);const n=track(c.createInstance());p.appendChild(n);n.name='Instance / '+k;
  const actual={};for(const[key,value]of Object.entries(props)){const prop=Object.keys(n.componentProperties).find(x=>x.split('#')[0]===key);if(prop)actual[prop]=value;}
  if(Object.keys(actual).length)n.setProperties(actual);if(w)n.resize(w,n.height);return n;
}
function button(p,label,to=null,kind='secondary',w=null){
  const width=w??p.width-p.paddingLeft-p.paddingRight,n=inst(p,kind,{Label:label},width);dim(n,width,48);
  const t=n.findAllWithCriteria({types:['TEXT']})[0];if(t){t.resize(Math.max(16,width-24),24);t.textAutoResize='HEIGHT';t.textAlignHorizontal='CENTER';}
  return kind==='disabled'?n:click(n,to);
}
function icon(p,key,size=24){const n=inst(p,'icon/'+key);dim(n,size,size);return n;}
function photo(p,kind,w,h,r=16){const n=track(figma.createFrame());p.appendChild(n);n.name='Photo / illustrative '+kind;dim(n,w,h);n.cornerRadius=r;n.clipsContent=true;n.fills=[{type:'IMAGE',imageHash:BASE.images[kind]||BASE.images.cat,scaleMode:'FILL'}];return n;}
function field(p,label,value,w=null){
  const width=w??p.width-p.paddingLeft-p.paddingRight,n=inst(p,'field',{Label:label,Value:value},width);dim(n,width,82);
  for(const c of n.children){if(c.type==='TEXT'){c.resize(width,c.height);c.textAutoResize='HEIGHT';}else if(c.type==='FRAME'){c.resize(width,c.height);for(const t of c.findAllWithCriteria({types:['TEXT']})){t.resize(Math.max(24,width-32),t.height);t.textAutoResize='HEIGHT';}}}return n;
}
function note(p,title,body,tone='subtle'){const n=box(p,'Notice / '+title,p.width-p.paddingLeft-p.paddingRight,16,8,tone,16);text(n,title,'Strong');text(n,body,'Label','muted');return n;}
function line(p,label,value){const w=p.width-p.paddingLeft-p.paddingRight,n=box(p,'Fact / '+label,w,0,12,null,0,'HORIZONTAL');const lw=Math.min(104,w*.3);text(n,label,'Label','muted',lw);text(n,value,'Body','ink',w-lw-12);return n;}
function actionRow(p,items){const w=p.width-p.paddingLeft-p.paddingRight,n=box(p,'Actions',w,0,12,null,0,'HORIZONTAL');const bw=(w-12*(items.length-1))/items.length;for(const a of items)button(n,a[0],a[1],a[2]||'secondary',bw);return n;}
function chips(p,labels,active=0){const n=box(p,'Filters',p.width-p.paddingLeft-p.paddingRight,0,8,null,0,'HORIZONTAL');for(let i=0;i<labels.length;i++){const w=Math.max(80,labels[i].length*16+40);const c=inst(n,i===active?'chipSelected':'chip',{Label:labels[i]},w);dim(c,w,48);}return n;}
function row(p,title,sub,to){const w=p.width-p.paddingLeft-p.paddingRight,n=inst(p,'row',{Title:title,Value:sub},w);n.layoutSizingVertical='HUG';const copy=n.children.find(c=>c.type==='FRAME');if(copy){copy.resize(Math.max(80,w-32),copy.height);for(const t of copy.findAllWithCriteria({types:['TEXT']})){t.resize(Math.max(80,w-32),t.height);t.textAutoResize='HEIGHT';}}return click(n,to);}
function pet(p,w=undefined,kind='cat',title='小满',meta='武汉 · 约 8 个月 · 母猫',to='detail'){
  const n=inst(p,'crossPet',{Title:title,Meta:meta,Summary:'救助领养 · 必付费用声明 ¥0'},w||p.width-p.paddingLeft-p.paddingRight);n.layoutSizingVertical='HUG';
  const image=n.children.find(c=>c.name==='Pet photo');if(image){image.resize(n.width,Math.min(240,n.width*.64));image.fills=[{type:'IMAGE',imageHash:BASE.images[kind]||BASE.images.cat,scaleMode:'FILL'}];}
  const copy=n.children.find(c=>c.name==='Pet copy');if(copy){copy.resize(n.width,copy.height);for(const t of copy.findAllWithCriteria({types:['TEXT']})){t.resize(n.width-32,t.height);t.textAutoResize='HEIGHT';}}
  return click(n,to);
}
function scroll(p,name,w,h,pad=24,gap=24){const viewport=box(p,name,w,0,0);dim(viewport,w,h);viewport.clipsContent=true;viewport.overflowDirection='VERTICAL';return box(viewport,name+' / content',w,pad,gap);}
function heading(p,title,sub){text(p,title,'Title');if(sub)text(p,sub,'Label','muted');}
const navItems=[['home','社区','house'],['adopt','领养','heart-handshake'],['compose','发布','circle-plus'],['messages','消息','message-circle'],['mine','我的','user-round']];
function navItem(p,key,label,ic,active,w){const n=inst(p,active?'navActiveCross':'navDefaultCross',{Label:label,Icon:comps['icon/'+ic].id},w);dim(n,w,52);return click(n,key);}
function shell(root,kind,title){
  const W=root.width,H=root.height,isTablet=PLATFORM==='tablet',isWeb=PLATFORM==='web',narrow=W<768;
  root.layoutMode='VERTICAL';root.itemSpacing=0;root.fills=[paint('background')];root.clipsContent=true;root.cornerRadius=isTablet?20:0;root.setExplicitVariableModeForCollection(BASE.collections.color,CONFIG.modeId);
  const top=box(root,'Application header',W,16,16,'surface',0,'HORIZONTAL');dim(top,W,narrow?64:80);top.counterAxisAlignItems='CENTER';
  text(top,'M&D','MobileBrand','ink',96);if(!narrow)text(top,'meow & doggie','Caption','muted',136);
  if(isWeb&&!narrow){for(const[k,l]of navItems)button(top,l,k,k===kind?'secondary':'quiet',80);button(top,'武汉 · 上海','lost','quiet',128);button(top,'登录 / 注册','login','primary',144);}
  else{const space=box(top,'Flexible title',Math.max(24,W-(narrow?252:616)),0,0);text(space,narrow?'领养详情':title,'Label','muted');if(!narrow)button(top,'武汉','lost','quiet',88);button(top,narrow?'返回':'搜索','home','quiet',narrow?92:100);}
  const area=box(root,'Workspace',W,0,0,null,0,narrow?'VERTICAL':'HORIZONTAL');dim(area,W,H-top.height);
  if(!isWeb&&!narrow){const rw=isTablet?88:208,rail=box(area,'Persistent navigation',rw,16,12,'surface');dim(rail,rw,area.height);for(const[k,l,ic]of navItems){const n=navItem(rail,k,isTablet?'':l,ic,k===kind,rw-32);if(isTablet){n.name='Navigation / '+l;const t=n.findAllWithCriteria({types:['TEXT']})[0];if(t)t.visible=false;}}
    if(!isTablet){text(rail,'同城互助','Caption','muted');row(rail,'走失互助','武汉 / 上海','lost');row(rail,'设置与隐私','账号与通知','settings');text(rail,'学习示意 · 非真实数据','Caption','muted');}else{button(rail,'互助','lost','quiet',56);}
    return {p:area,w:W-rw,h:area.height,narrow:false};}
  if(isWeb&&!narrow){area.primaryAxisAlignItems='CENTER';return {p:area,w:Math.min(W,1248),h:area.height,narrow};}
  return {p:area,w:W,h:area.height,narrow};
}
function feedCard(p,w,kind,title,owner){
  const n=inst(p,'socialFeedTile',{Title:title,Owner:owner,Comments:'3'},w);n.layoutSizingVertical='HUG';
  for(const c of n.children){if(c.type==='FRAME'){c.resize(w,c.height);if(c.name==='Photo / social'){c.resize(w,Math.min(280,w*.9));c.fills=[{type:'IMAGE',imageHash:BASE.images[kind]||BASE.images.cat,scaleMode:'FILL'}];}else if(c.name==='Post copy'){for(const t of c.findAllWithCriteria({types:['TEXT']})){t.resize(w-16,t.height);t.textAutoResize='HEIGHT';}}}}
  return click(n,'detail');
}
function feed(p,w,h,landscape=false){
  const main=scroll(p,'Community stream',w,h);heading(main,'晒晒日常，也聊聊养宠的小事','照片和心情，都有宠友回应。');chips(main,['推荐','关注','宠友圈','同城']);
  const composer=box(main,'Quick composer',w-48,16,12,'surface',16,'HORIZONTAL');const av=inst(composer,'socialAvatar');dim(av,48,48);text(composer,'晒晒今天的小可爱…','Body','muted',w-180);click(composer,'compose');icon(composer,'camera');
  const cols=w>=1000?3:2,cw=(w-48-16*(cols-1))/cols,posts=[['catOffer','太阳晒到肚皮啦','布丁的铲屎官'],['cat','小满午后的打盹时间','小满的朋友'],['dog','出门遛弯前的小仪式','奶糖的朋友'],['rabbit','小小的陪伴，也很治愈','团子的朋友'],['cat','今天也在认真长大','小满的朋友'],['catOffer','给猫咪留一个安静角落','布丁的铲屎官']];
  for(let i=0;i<posts.length;i+=cols){const r=box(main,'Feed row',w-48,0,16,null,0,'HORIZONTAL');for(const a of posts.slice(i,i+cols))feedCard(r,cw,...a);}
  row(main,'走失互助','查看武汉、上海的公开求助','lost');
}
function summary(p,w,title='小满，想找一个安稳的家'){
  photo(p,'cat',w,Math.min(320,w*.62));heading(p,title,'救助领养 · 资料示例');const facts=box(p,'Pet disclosure',w,16,16,'surface',16);
  line(facts,'位置','武汉 · 武昌区');line(facts,'基本信息','母猫 · 约 8 个月');line(facts,'健康情况','疫苗由发布者声明，医疗原件未核对');line(facts,'必付费用','声明为 ¥0，不附带购买要求');
  note(p,'每一项资料，分开了解','身份、关系、地点与费用按事项核对；待核对不代表违规，也不能当作保证。');
  row(p,'持续介绍与近况','看看小满最近的生活','mine');row(p,'领养讨论与答疑','先了解照护条件','messages');
}
function detail(p,w,h,narrow=false){
  if(narrow){const c=scroll(p,'Shared pet detail',w,h-80,16,16);photo(c,'cat',w-32,220);heading(c,'小满，想找一个安稳的家','救助领养 · 武汉 · 资料示例');note(c,'必付费用声明 ¥0','疫苗由发布者声明；医疗原件未核对。');row(c,'真实性说明','逐项看范围','detail');row(c,'领养条件','室内照护，接受后续沟通','apply');const f=box(p,'Bottom actions',w,16,12,'surface',0,'HORIZONTAL');dim(f,w,80);button(f,'先聊一聊','login','secondary',(w-44)/2);button(f,'申请领养','login','primary',(w-44)/2);return;}
  const cw=Math.min(720,w-48-(w>950?320:0)),body=scroll(p,'Pet reading',cw+48,h);summary(body,cw);
  if(w>950){const side=scroll(p,'Reading context',w-cw-48,h,24,16);note(side,'发布者','小满的照护人 · 与宠物关系待核对');actionRow(side,[['先聊一聊','messages'],['申请领养',PLATFORM==='web'?'login':'apply','primary']]);row(side,'收藏这只宠物','可以稍后再了解','mine');row(side,'报告问题','描述具体事项','settings');note(side,'到见面之前','先在站内沟通；双方确认同一版本的预约再出发。');}else actionRow(body,[['先聊一聊','messages'],['申请领养','apply','primary']]);
}
function adoption(p,w,h,split=false){
  if(split){const lw=320,left=scroll(p,'Pet results',lw,h,24,16);heading(left,'领养','武汉 · 可调整条件');chips(left,['猫咪','狗狗']);pet(left,lw-48);pet(left,lw-48,'dog','奶糖','武汉 · 小型犬','detail');const right=scroll(p,'Selected pet details',w-lw,h,24,16);summary(right,w-lw-48);actionRow(right,[['先聊一聊','messages'],['申请领养','apply','primary']]);return;}
  const c=scroll(p,'Adoption browsing',w,h);heading(c,'让相遇，多一点了解','先看介绍，再决定是否联系。');chips(c,['猫咪','狗狗','其他宠物']);chips(c,['武汉','上海','年龄','健康情况']);
  const cols=w>=1080?3:2,cw=(w-48-16*(cols-1))/cols;
  const animals=[['cat','小满','武汉 · 约 8 个月 · 母猫'],['catOffer','布丁','武汉 · 成年猫 · 公猫'],['dog','奶糖','上海 · 小型犬 · 已成年'],['cat','小满的近况','武汉 · 持续介绍'],['rabbit','团子','上海 · 兔子 · 资料待补充'],['catOffer','橘子','上海 · 成年猫']];
  for(let i=0;i<animals.length;i+=cols){const r=box(c,'Pet cards',w-48,0,16,null,0,'HORIZONTAL');for(const a of animals.slice(i,i+cols))pet(r,cw,...a);}
}
function application(p,w,h){const c=scroll(p,'Private adoption application',Math.min(760,w),h,32,24);heading(c,'申请认识小满','1 / 2 · 了解彼此，不是考试');field(c,'居住与照护安排','请选择实际安排');field(c,'共同居住者意见','请选择，不需要上传身份证');field(c,'已有宠物','没有 / 有，按物种补充');field(c,'想让照护者了解你（可选）','简单介绍照护计划');note(c,'只向对方提供必要摘要','不公开住址、身份证和材料原件；不同意额外要求可以退出。');actionRow(c,[['暂存草稿','mine'],['检查申请摘要','applyReview','primary']]);if(w>960){const s=scroll(p,'Application context',w-760,h,24,16);pet(s);note(s,'可以慢慢决定','申请不等于接受；可以撤回，也可以在沟通后再决定。');}}
function lost(p,w,h,isDetail=false){const c=scroll(p,'Local help',w,h);heading(c,isDetail?'小满 · 走失求助示例':'走失互助','武汉 / 上海 · 公开案例示例');if(!isDetail){chips(c,['武汉','上海','正在寻找','已回家']);note(c,'用可靠的信息，帮它回家','事件时间与最后维护时间分开显示；精度不足的地点不提供导航。');
    const r=box(c,'Help content',w-48,0,24,null,0,'HORIZONTAL'),lw=Math.min(360,(w-72)/2),list=box(r,'Help results',lw,0,16);pet(list,lw,'cat','小满 · 寻找中','武汉 · 武昌区 · 10 月 19 日','lostDetail');pet(list,lw,'dog','奶糖 · 拾获待认领','上海 · 浦东新区 · 10 月 19 日','lostDetail');
    const area=box(r,'Area summary',w-72-lw,24,16,'surface',16);text(area,'武汉 · 武昌区','Section');text(area,'区域示意，不提供真实坐标或地图导航。','Body','muted');note(area,'附近提醒需主动开启','范围和通知独立设置；不展示持续后台定位。');button(area,'设置附近提醒','settings');button(area,'发布走失 / 拾获求助','compose','primary');
  }else{const r=box(c,'Help and private clue',w-48,0,24,null,0,'HORIZONTAL'),lw=w>900?Math.round((w-72)*.46):Math.round((w-72)*.42),left=box(r,'Public case',lw,0,16);photo(left,'cat',lw,230);line(left,'事件时间','10 月 19 日下午（示例）');line(left,'公开地点','武汉 · 武昌区，非精确地址');line(left,'维护记录','发布者更新 · 寻找中');note(left,'仍在寻找','无效线索与已找回需要发布者另行确认。');
    const right=box(r,'Private clue form',w-72-lw,24,16,'surface',16);text(right,'提供我看到的线索','Section');field(right,'看到的时间','请选择时间');field(right,'地点与特征','仅对发布者展示');field(right,'现场照片（可选）','选择照片');note(right,'线索私密发送','坐标、联系方式与认领特征不进入公开动态；拾获认领材料单独处理。');button(right,'检查私密线索','clueReview','primary');}
}
function conversationList(p,w,h,activeGroup=false){const c=scroll(p,'Conversation list',w,h,16,12);text(c,'消息','Section');chips(c,['私信','群聊']);for(const[name,msg,to]of [['小满的照护人','V2 见面安排已确认','messages'],['小满和朋友们','奶糖：今天阳光真好','group'],['互动与通知','有人回复了你的日记','mine'],['消息请求','未确认前不显示更多资料','settings']]){const n=box(c,'Conversation / '+name,w-32,12,8,(activeGroup?to==='group':to==='messages')?'brandSoft':'surface',16);text(n,name,'Strong');text(n,msg,'Label','muted');click(n,to);}}
function chat(p,w,h,group=false){const listW=w<800?252:280;conversationList(p,listW,h,group);const contextW=w>=1000?280:0,cw=w-listW-contextW;
  const panel=box(p,'Active conversation',cw,0,0,'surface');dim(panel,cw,h);const head=box(panel,'Chat header',cw,16,8,'surface');dim(head,cw,88);text(head,group?'小满和朋友们':'小满的照护人','Strong');text(head,group?'8 位成员 · 当前为普通成员':'救助领养 · 站内沟通','Caption','muted');
  const body=scroll(panel,'Message history',cw,h-200,24,16);text(body,'10 月 20 日 · 学习示例','Caption','muted');note(body,group?'宠友群约定':'关于小满的领养','友善交流，只分享必要的信息。');
  for(const[mine,msg]of [[false,group?'今天带奶糖去晒太阳了。':'你好，小满目前在武汉，方便先聊聊照护安排吗？'],[true,group?'小满也在窗边睡着了。':'可以的，我想先了解疫苗情况和见面安排。'],[false,group?'有空一起分享近况呀。':'可以，疫苗是我的声明，医疗原件还没有核对。']]){const b=box(body,mine?'Message / me':'Message / them',cw-48,16,8,mine?'brandSoft':'subtle',16);text(b,msg,'Body');}
  if(!group){const n=note(body,'见面安排 · V2 已确认','10 月 22 日 16:00 · 解放公园南门（示例）');click(n,'appointment');}
  const input=box(panel,'Docked composer',cw,16,8,'surface');dim(input,cw,112);const r=box(input,'Composer controls',cw-32,0,8,null,0,'HORIZONTAL');icon(r,'camera');const e=inst(r,'expressionAsset/smile');dim(e,24,24);const draft=box(r,'Draft field',Math.max(80,cw-192),8,0,'subtle',16);text(draft,'写点什么…','Body','muted');button(r,'发送',null,'disabled',72);text(input,'Enter 发送 · Shift+Enter 换行','Caption','muted');
  if(contextW){const side=scroll(p,'Conversation context',contextW,h,16,16);if(group){note(side,'群资料','小满和朋友们 · 8 位成员');row(side,'成员与公告','普通成员可查看','group');note(side,'管理权限','转让与解散分别处理；普通成员无群主操作。');}else{pet(side,contextW-32);row(side,'见面安排','V2 已确认','appointment');note(side,'保留同一版本','时间、地点、接待方或费用变更，需要双方重新确认。');}}
}
function compose(p,w,h,keyboard=false){const narrow=w<900,cw=narrow?w:Math.min(740,w-320),c=scroll(p,'Photo diary editor',cw,h,24,16);heading(c,'记录今天的小可爱','图文日记 · 草稿仅自己可见');field(c,'标题','小满今天又占领了窗边');field(c,'正文','阳光刚好，和小满一起慢慢过一天。');
  const r=box(c,'Photo uploads',cw-48,0,12,null,0,'HORIZONTAL'),pw=(cw-72)/3;photo(r,'cat',pw,140);photo(r,'catOffer',pw,140);const failed=box(r,'Failed photo',pw,16,8,'subtle',16);dim(failed,pw,140);text(failed,'第 3 张失败','Label');button(failed,'重试',null,'secondary',pw-32);
  note(c,'其他照片已保留','可重试、移除或调整顺序；失败图片不会悄悄丢失。');chips(c,['公开','宠友圈','仅自己'],2);actionRow(c,[['保存草稿','mine'],['预览发布','home','primary']]);
  if(keyboard){const panel=box(c,'Docked expression panel',cw-48,16,12,'subtle',16);text(panel,'表情 · 最近 · 收藏','Label');const rr=box(panel,'Native emoji choices',cw-80,0,16,null,0,'HORIZONTAL');for(const k of ['smile','heart','cat']){const hit=box(rr,'48px expression target',48,8,0);dim(hit,48,48);const e=inst(hit,'expressionAsset/'+k);dim(e,32,32);}text(panel,'收起后保留草稿 · 外接键盘不占用软键盘区域','Caption','muted');}
  if(!narrow){const side=scroll(p,'Publishing context',w-cw,h,24,16);text(side,'发布前看一眼','Section');pet(side,w-cw-48);note(side,'关联宠物','本篇关联小满；不同宠物的可见范围单独设置。');note(side,'草稿与权限','隐私材料不带入公开发布；当前不提供视频入口。');}
}
function appointment(p,w,h){const cw=Math.min(760,w),c=scroll(p,'Appointment version',cw,h,24,16);heading(c,'见面安排 · V2','双方已确认 · 变更需要新版本');const f=box(c,'Confirmed appointment',cw-48,24,16,'surface',16);line(f,'当前版本','V2 · 已生效');line(f,'时间','10 月 22 日 16:00（示例）');line(f,'地点','解放公园南门（示例）');line(f,'接待方','小满的照护人 · 本人');line(f,'费用','领养声明 ¥0');line(f,'双方','均已确认同一版本');note(c,'恢复旧时间，也要重新确认','V1 保留为历史记录；提出 V3 后，不能直接把旧 V1 恢复为生效。');actionRow(c,[['提出变更','messages'],['取消预约','messages']]);row(c,'返回会话','继续站内沟通','messages');if(w>900){const s=scroll(p,'Version history',w-cw,h,24,16);text(s,'版本记录','Section');note(s,'V2 · 当前生效','10 月 22 日 16:00 · 双方确认');note(s,'V1 · 已被替代','原时间保留供查阅，不可直接恢复');note(s,'下一步','如有变更，提出 V3 并等待双方确认。');}}
function mine(p,w,h){const c=scroll(p,'My pet spaces',w,h);heading(c,'小满的朋友','M&D · meow & doggie · 学习账号');chips(c,['我的宠物','宠友圈','收藏','草稿']);const r=box(c,'Pet spaces',w-48,0,16,null,0,'HORIZONTAL'),pw=(w-64)/2;pet(r,pw,'cat','小满','我的宠物 · 照护记录','detail');pet(r,pw,'dog','奶糖','我的宠物 · 独立可见范围','detail');const rows=box(c,'Personal tasks',w-48,24,16,'surface',16);row(rows,'我的领养申请','进度、撤回与后续关怀','apply');row(rows,'我的发布和草稿','图文、领养介绍与互助','compose');row(rows,'预约与交接','查看当前生效版本','appointment');row(rows,'设置与隐私','账号、通知、授权与材料','settings');}
function settings(p,w,h){const c=scroll(p,'Settings and privacy',Math.min(800,w),h);heading(c,'设置与隐私','每种通知分别管理。');const card=box(c,'Notification categories',c.width-48,24,16,'surface',16);for(const [label,value]of [['私信','已开启'],['群聊','仅提醒提到我'],['互动通知','已开启'],['预约与交接','已开启'],['附近互助提醒','未开启 · 单独设置']])line(card,label,value);note(c,'系统权限与站内开关分开','系统权限未授予时，保留站内消息；附近提醒需主动选择范围。');row(c,'登录方式与关联账号','首次创建与关联已有账号分开','login');row(c,'角色与协作权限','介绍协作者与照护者权限不互通','mine');row(c,'私密材料与处理结果','核实范围、过期与补充材料分开','lostDetail');row(c,'退出登录','显示影响后再确认','login');}
function auth(p,w,h,otp=false){const cw=Math.min(540,w),left=scroll(p,'Authentication',cw,h,32,24);heading(left,otp?'输入验证码':'和毛孩子，一起长大',otp?'已发送至 138 **** 0000 · 固定样例':'登录后，继续申请认识小满。');
  if(otp){field(left,'验证码','••••••');note(left,'返回后仍保留原任务','成功后回到申请摘要，不进入无关首页。');button(left,'演示：回到申请','apply','primary');button(left,'重新获取验证码',null,'disabled');}
  else{field(left,'中国大陆手机号','请输入手机号');note(left,'服务协议与隐私政策','须主动勾选后才能获取验证码；这里只展示固定状态。');button(left,'获取验证码',null,'disabled');button(left,'查看验证码状态','otp','quiet');actionRow(left,[['微信',null],['QQ',null]]);row(left,'已有账号，去关联','不自动创建第二个账号','settings');button(left,'先逛逛','detail','quiet');}
  if(w>800){const side=scroll(p,'Task return context',w-cw,h,32,24);photo(side,'cat',Math.min(w-cw-64,600),Math.min(360,w-cw-64));heading(side,'一次相遇，慢慢了解','meow & doggie');note(side,'你正在申请认识小满','取消登录返回详情；成功登录返回申请，不丢失当前宠物上下文。');text(side,'三方入口为示意；未执行授权。平台能力确认后再接入。','Caption','muted');}}
function unavailable(p,w,h,offline=false){const c=scroll(p,'Recovery state',w,h,48,24);heading(c,offline?'暂时离线，草稿还在':'这条分享已撤回',offline?'本机保存不等于已经同步到其他设备。':'失效链接不展示旧照片、位置或联系方式。');note(c,offline?'本机草稿 · 未同步':'可以继续看看',offline?'重新连接后先检查版本差异，再决定是否提交；不会自动覆盖其他设备的更新。':'返回领养列表，寻找仍然公开的介绍。');actionRow(c,offline?[['继续编辑','compose','primary'],['查看我的草稿','mine']]:[['返回领养列表','adopt','primary'],['返回社区','home']]);}
function review(p,w,h,clue=false){const c=scroll(p,'Private summary review',Math.min(w,800),h,32,24);heading(c,clue?'检查私密线索':'检查申请摘要','固定示例 · 提交前再次确认');note(c,clue?'接收方：求助发布者':'接收方：小满的照护人',clue?'仅包含本次时间、地点描述和照片。':'仅包含本次居住与照护安排摘要。');line(c,'公开范围','不进入公开动态');line(c,'资料状态','示例内容，尚未发送');note(c,'请确认资料真实并同意本次分享','确认前不可提交；本稿没有实际发送、任意输入或服务端处理。');button(c,clue?'发送私密线索':'发送申请',null,'disabled');button(c,'返回修改',clue?'lostDetail':'apply');}
for(const[key,title,W,H]of BATCH){
  const root=await figma.getNodeByIdAsync(STATE.roots[PLATFORM+'/'+key]);if(!root)throw Error('Missing root '+key);if(root.children.length)throw Error('Already rendered '+key);
  root.name=PLATFORM+' / '+title+' ['+key+']';mutatedNodeIds.push(root.id);roots[key]=root.id;
  let base=key.replace(/[PL]$/,'');if(key==='mobileDetail'||key==='narrowDetail')base='detail';const n=shell(root,base,title),p=n.p,w=n.w,h=n.h;
  if(base==='home')feed(p,w,h,key.endsWith('L'));
  else if(base==='adopt')adoption(p,w,h,(PLATFORM==='desktop'||key==='adoptL'));
  else if(base==='detail')detail(p,w,h,n.narrow);
  else if(base==='apply')application(p,w,h);
  else if(base==='applyReview'||base==='clueReview')review(p,w,h,base==='clueReview');
  else if(base==='lost')lost(p,w,h);
  else if(base==='lostDetail')lost(p,w,h,true);
  else if(base==='messages'||base==='group')chat(p,w,h,base==='group');
  else if(base==='compose')compose(p,w,h,key==='composeL');
  else if(base==='appointment')appointment(p,w,h);
  else if(base==='mine')mine(p,w,h);
  else if(base==='settings')settings(p,w,h);
  else if(base==='login'||base==='otp')auth(p,w,h,base==='otp');
  else unavailable(p,w,h,base==='offline');
  root.placeholder=false;
}
await Promise.all(jobs);
// Resolve only to the same platform. Small/portrait tablet destinations use their dedicated roots.
const map=Object.fromEntries(CONFIG.inventory[PLATFORM].map(([k])=>[k.replace(/[PL]$/,''),STATE.roots[PLATFORM+'/'+k]]));
if(PLATFORM==='tablet'){Object.assign(map,{home:STATE.roots['tablet/homeP'],adopt:STATE.roots['tablet/adoptL'],detail:STATE.roots['tablet/detailP'],messages:STATE.roots['tablet/messagesL'],compose:STATE.roots['tablet/composeP'],mine:STATE.roots['tablet/mineP'],login:STATE.roots['tablet/loginP'],lost:STATE.roots['tablet/lostL'],appointment:STATE.roots['tablet/appointmentL']});}
const unresolved=[];for(const l of links){const target=map[l.to];const node=await figma.getNodeByIdAsync(l.id);let source=node;while(source.parent&&source.parent.type!=='PAGE')source=source.parent;if(target&&target!==source.id)node.reactions=[{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:target,navigation:'NAVIGATE',transition:null}]}];else if(!target)unresolved.push(l);}
const evidence=Object.entries(roots).map(([key,id])=>{const n=page.children.find(n=>n.id===id),all=n.findAll(()=>true),types={};for(const a of all)types[a.type]=(types[a.type]||0)+1;return {key,id,name:n.name,w:n.width,h:n.height,nodes:all.length,types,images:all.filter(a=>'fills'in a&&Array.isArray(a.fills)&&a.fills.some(f=>f.type==='IMAGE')).map(a=>({id:a.id,name:a.name,type:a.type,w:a.width,h:a.height})),fonts:[...new Set(all.filter(a=>a.type==='TEXT').map(a=>a.fontName.family))]};});
return {platform:PLATFORM,pageId:page.id,roots,createdNodeIds,mutatedNodeIds,removedNodeIds,linkCount:links.length,unresolved,evidence};
