const wrapper=await figma.getNodeByIdAsync('352:3');
if(wrapper.children.some(n=>n.name==='01 / 三类消息中心'))return {skipped:true,reason:'Inbox review row already exists; inspect it before repair.'};
badgeSet.setExplicitVariableModeForCollection('VariableCollectionId:14:6','56:0');
badgeSet.setExplicitVariableModeForCollection('VariableCollectionId:70:148','70:0');
rowSet.setExplicitVariableModeForCollection('VariableCollectionId:14:6','56:0');
rowSet.setExplicitVariableModeForCollection('VariableCollectionId:70:148','70:0');
const gallery=pad(frame(wrapper,'01 / 三类消息中心','HORIZONTAL',1256,null),0,32);gallery.counterAxisAlignItems='MIN';
const screens={};
const chat=phone(gallery,'UNREAD-01 / 聊天与全局99+');screens.chat=chat.root.id;
tabs(chat.body,0);
const tools=frame(chat.body,'Inbox filter','HORIZONTAL',358,40);tools.counterAxisAlignItems='CENTER';txt(tools,'全部消息','Label');const space=frame(tools,'Flexible space','HORIZONTAL',1,1);space.layoutSizingHorizontal='FILL';const filter=button(tools,'仅看未读',false,100);filter.name='Action / 仅看未读';
const chatRows=[
  {kind:'Notice',title:'消息请求',preview:'有新朋友想和你交流',meta:'3条未读 · 接受后才能发图片',count:3,time:'刚刚',icon:messageSource},
  {title:'布丁的铲屎官',preview:'原来你家也喜欢晒太阳呀。',meta:'日常交流',count:6,time:'10:24'},
  {title:'小满的照护者',preview:'周六下午的安排可以吗？',meta:'领养交流 · 小满',count:12,time:'10:20'},
  {kind:'Notice',title:'武汉猫友日常',preview:'[有人@我] 布丁：周末一起晒猫',meta:'62条未读 · 含3次@我',count:62,time:'10:18',icon:messageSource},
  {kind:'Notice',title:'猫咪好物交流',preview:'免打扰 · 团子分享了一张照片',meta:'不计入全局数字（设计建议）',count:124,time:'昨天',muted:true,icon:bellSource}
];
const rowIds=[];for(const data of chatRows)rowIds.push(row(chat.body,data).id);
const interaction=phone(gallery,'UNREAD-02 / 互动分类');screens.interaction=interaction.root.id;tabs(interaction.body,1);
note(interaction.body,'与你有关的回应','帖子、宠友圈与日记的回复统一在这里，不会漏掉对评论的回复。');
const interactionRows=[
  {kind:'Notice',title:'回复我',preview:'布丁回复了你的评论',meta:'小满午后的打盹时间',count:8,time:'10:24',icon:messageSource},
  {kind:'Notice',title:'@我',preview:'奶糖在一篇日常里提到了你',meta:'查看对应内容和提及位置',count:3,time:'10:10',icon:messageSource},
  {kind:'Notice',title:'赞与收藏',preview:'团子等5位宠友喜欢你的分享',meta:'查看新互动',count:5,time:'09:58',icon:heartSource}
];
const interactionRowIds=interactionRows.map(data=>row(interaction.body,data).id);
note(interaction.body,'看到哪一条，就读哪一条','打开本页不会清空全部互动；点进对应内容后再更新已读。','surface');
const notice=phone(gallery,'UNREAD-03 / 通知与待处理');screens.notice=notice.root.id;tabs(notice.body,2);
const noticeRows=[
  {kind:'Notice',title:'好友与邀请',preview:'奶糖希望添加你为宠友',meta:'3条未读 · 5项仍待处理',count:3,time:'10:22',icon:heartSource},
  {kind:'Notice',title:'业务进度',preview:'有新的预约提案和领养进展',meta:'预约、领养、走失线索与附近提醒',count:4,time:'10:15',icon:adoptionSource},
  {kind:'Notice',title:'审核与系统',preview:'你提交的补充材料有新进度',meta:'发布审核、申诉、安全与服务通知',count:1,time:'09:50',icon:bellSource}
];
const noticeRowIds=noticeRows.map(data=>row(notice.body,data).id);
note(notice.body,'已读 ≠ 已处理','看过通知后未读会减少；待接受的好友申请、待确认的预约仍会保留。');
const pending=round(pad(frame(notice.body,'Pending work stays pending','VERTICAL',358,null,'surface'),16,8),16);
txt(pending,'仍待你决定','Strong',326);txt(pending,'好友申请 2 项  ·  邀请 1 项\n预约安排 2 项','Body',326);button(pending,'查看待处理',false,140);
return {createdNodeIds:created,mutatedNodeIds:[badgeSet.id,rowSet.id],screens,rowIds,interactionRowIds,noticeRowIds,galleryId:gallery.id,counts:{chat:83,interaction:16,notice:8,total:107,display:'99+'},fonts:['Noto Sans SC','Manrope']};
