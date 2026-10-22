// Reorganize current phone roots into native Sections. No roots are recreated.
const page=await figma.getNodeByIdAsync("56:25");await figma.setCurrentPageAsync(page);
await Promise.all([{family:"Noto Sans SC",style:"Regular"},{family:"Noto Sans SC",style:"Bold"}].map(f=>figma.loadFontAsync(f)));
const createdNodeIds=[],mutatedNodeIds=[],removedNodeIds=[],groups=[],jobs=[];
const variables=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(variables.map(v=>[v.id,v]));
function val(v){const x=v.valuesByMode[STATE.modeId]??Object.values(v.valuesByMode)[0];return x?.type==="VARIABLE_ALIAS"?val(byId[x.id]):x;}
function p(key){const v=byId[BASE.variables[key]||STATE.extraVariables?.[key]],c=val(v);return figma.variables.setBoundVariableForPaint({type:"SOLID",color:{r:c.r,g:c.g,b:c.b}},"color",v);}
function textAt(parent,text,x,y,w,role="Section",color="ink"){const n=figma.createText();parent.appendChild(n);n.name="Canvas guide / "+text.slice(0,40);n.fontName={family:"Noto Sans SC",style:role==="Title"||role==="Section"?"Bold":"Regular"};n.characters=text;n.resize(w,40);n.textAutoResize="HEIGHT";n.x=x;n.y=y;n.fills=[p(color)];createdNodeIds.push(n.id);jobs.push(n.setTextStyleIdAsync(BASE.styles[role]||BASE.styles.Body).then(()=>{n.textAutoResize="HEIGHT";}));return n;}
const names=["00 设计总入口","01 社区与宠友圈","02 我的宠物与日记","03 领养与照护更新","04 走失互助与附近提醒","05 私信与宠友群","06 账号隐私与通用异常"];
const descriptions=["先从总入口体验；旁边是三条核心闭环图。点击模块入口可串联，画布按模块、子流程和状态编号。","公开社区与双向宠友圈分开；朋友范围、逐条权限、评论和回复保留具体对象。","一人多宠，每只宠物独立资料与图文日记；档案、单条日记、公开求助分开授权。","领养为主，真实披露、持续介绍与答疑优先；申请、安排变更、交接、回访和复核衔接。","私人走失状态与公开事件分开；区域地图、私密线索、主人确认找回和停止提醒互相串联。","私信请求、群成员与群主流程分开；每个表情发送确认保持当前会话上下文。","登录、隐私、举报、申诉和共用异常；这些是固定状态评审稿，不代表真实服务或合规验收。"];
const placed=[];
for(let m=0;m<7;m++){
 const section=await figma.getNodeByIdAsync(STATE.sections[String(m)]);section.x=m*2460;section.y=0;
 const items=CATALOG.filter(x=>x.module===m);section.name=names[m]+" · "+items.length+"个参考状态";section.fills=[p("background")];
 for(const c of [...section.children])if(c.type==="TEXT"&&c.name===names[m]){removedNodeIds.push(c.id);c.remove();}
 textAt(section,names[m],48,32,2200,"Title");textAt(section,descriptions[m],48,96,2200,"Body","muted");
 if(m===0){const root=await figma.getNodeByIdAsync(STATE.roots.moduleHub);section.appendChild(root);root.name="00-A01 手机设计总入口";root.x=48;root.y=220;placed.push({key:"moduleHub",id:root.id,module:0,x:root.x,y:root.y});mutatedNodeIds.push(root.id);section.resizeWithoutConstraints(2326,2380);continue;}
 const groupNames=[...new Set(items.map(x=>x.group))].sort();let y=180;
 for(const group of groupNames){const list=items.filter(x=>x.group===group);textAt(section,group+" · "+list.length+"个状态",48,y,2200,"Section");y+=88;for(let i=0;i<list.length;i++){const d=list[i],root=await figma.getNodeByIdAsync(d.id);section.appendChild(root);const prefix=String(m).padStart(2,"0")+"-"+group[0]+String(i+1).padStart(2,"0");root.name=prefix+" "+d.title+" ["+d.key+"]";root.x=48+(i%5)*446;root.y=y+Math.floor(i/5)*948;textAt(section,prefix+" "+d.title,root.x,root.y-36,390,"Caption","muted");mutatedNodeIds.push(root.id);placed.push({key:d.key,id:root.id,module:m,group,x:root.x,y:root.y});}const h=Math.ceil(list.length/5)*948;groups.push({module:m,name:group,count:list.length,y});y+=h+100;}section.resizeWithoutConstraints(2326,y+24);mutatedNodeIds.push(section.id);
}
const hub=await figma.getNodeByIdAsync(STATE.sections["0"]);
textAt(hub,"三条关键闭环 · 点卡片进入对应状态",510,220,1700,"Section");
const paths=[
 ["宠友圈 · 日常到互动",["确认宠友","circleFriends"],["写图文、选宠物","circleCompose"],["逐条确认范围","circleAudience"],["预览后发布","circlePreview"],["评论与表态","circleComments"],["收紧权限或删除","circleOwnPostSettings"]],
 ["领养 · 披露到后续关怀",["真实披露与条件","adoptionConditions"],["持续图文与答疑","adoptionUpdates"],["申请与沟通","apply"],["安排重确认","appointment"],["交接与回访","handover"],["举报、回应与复核","reportProgress"]],
 ["走失 · 从标记到回家",["标记私人走失","petMarkMissing"],["授权公开求助","lostFromPetDisclosure"],["地图与附近提醒","lostAreaMap"],["私密线索核实","lostLeadInbox"],["主人确认找回","lostFoundConfirm"],["停止提醒与卡片更新","lostFoundDone"]]
];
let y=300;
for(const [title,...steps]of paths){textAt(hub,title,510,y,1700,"Section");for(let i=0;i<steps.length;i++){const[label,key]=steps[i];const card=figma.createAutoLayout("VERTICAL");hub.appendChild(card);card.name="Flow step / "+label;card.resize(248,156);card.layoutSizingHorizontal="FIXED";card.layoutSizingVertical="FIXED";card.x=510+i*282;card.y=y+64;card.paddingLeft=20;card.paddingRight=20;card.paddingTop=20;card.paddingBottom=20;card.itemSpacing=12;card.fills=[p("surface")];card.cornerRadius=16;const number=textAt(card,String(i+1).padStart(2,"0"),0,0,208,"Caption","brand");const labelNode=textAt(card,label,0,0,208,"Body");labelNode.layoutSizingVertical="HUG";card.reactions=[{trigger:{type:"ON_CLICK"},actions:[{type:"NODE",destinationId:STATE.roots[key],navigation:"NAVIGATE",transition:null}]}];createdNodeIds.push(card.id);if(i<steps.length-1)textAt(hub,"→",card.x+252,card.y+56,28,"Section","muted");}y+=280;}
textAt(hub,"评审顺序：先体验主线，再检查取消、失败、权限变化和结果。各模块最后保留共享表达及边界状态。",48,1160,2200,"Body");
textAt(hub,"范围：390×844 普通手机 · 珊瑚轻浅主版 · 原生文字/组件/图片 · 视频延期 · 不改生产 Stitch 和应用代码",48,1220,2200,"Body","muted");
textAt(hub,"验证边界：固定数据与状态切换；没有真实键盘、短信、发布、定位、地图服务、推送或实机读屏验收。",48,1280,2200,"Body","muted");
const starts=[{nodeId:STATE.roots.moduleHub,name:"00 · 模块总入口"},{nodeId:STATE.roots.home,name:"01 · 社区主线"},{nodeId:STATE.roots.circleFeed,name:"01 · 宠友圈"},{nodeId:STATE.roots.petsList,name:"02 · 我的宠物与日记"},{nodeId:STATE.roots.detail,name:"03 · 领养闭环"},{nodeId:STATE.roots.lostFromPetManage,name:"04 · 走失与找回"},{nodeId:STATE.roots.nearbyAlertPreferences,name:"04 · 附近5–100公里提醒"},{nodeId:STATE.roots.messages,name:"05 · 消息与群聊"},{nodeId:STATE.roots.settings,name:"06 · 账号隐私与异常"}];
for(const f of page.flowStartingPoints){if(starts.some(x=>x.nodeId===f.nodeId))continue;const item=CATALOG.find(x=>x.id===f.nodeId);starts.push({...f,name:/^Flow \d+$/.test(f.name)?"补充 · "+item.title:f.name});}
page.flowStartingPoints=starts;
await Promise.all(jobs);page.selection=[await figma.getNodeByIdAsync(STATE.roots.moduleHub)];figma.viewport.scrollAndZoomIntoView([await figma.getNodeByIdAsync(STATE.sections["0"])]);
return{createdNodeIds,mutatedNodeIds,removedNodeIds,sections:STATE.sections,groups,flowStartingPoints:starts,phoneRootCount:placed.length};
