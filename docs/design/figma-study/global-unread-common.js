// Figma design source only; execute through use_figma, never an application runner.
const page = await figma.getNodeByIdAsync('352:2');
await figma.setCurrentPageAsync(page);
const created = [];
const mutated = [];
const variables = await figma.variables.getLocalVariablesAsync();
const vm = new Map(variables.map(v => [v.id, v]));
const styles = await figma.getLocalTextStylesAsync();
const sm = new Map(styles.map(s => [s.name, s]));
await Promise.all([
  {family:'Noto Sans SC',style:'Regular'}, {family:'Noto Sans SC',style:'Medium'},
  {family:'Noto Sans SC',style:'Bold'}, {family:'Manrope',style:'ExtraBold'}
].map(f => figma.loadFontAsync(f)));
const color = {bg:'VariableID:14:36',surface:'VariableID:14:37',subtle:'VariableID:14:38',ink:'VariableID:14:39',muted:'VariableID:14:40',brand:'VariableID:14:41',soft:'VariableID:14:43',border:'VariableID:14:50',primary:'VariableID:70:149',onPrimary:'VariableID:70:155',success:'VariableID:14:44',successSoft:'VariableID:14:45'};
function paint(name) { return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:0,g:0,b:0}},'color',vm.get(color[name])); }
function remember(node) {created.push(node.id);if('children' in node)for(const child of node.children)remember(child);return node;}
function frame(parent,name,dir,width,height,bg) {
  const n=figma.createAutoLayout(dir);parent.appendChild(n);created.push(n.id);n.name=name;n.resize(width,height||1);
  n.primaryAxisSizingMode=!height&&dir==='VERTICAL'?'AUTO':'FIXED';n.counterAxisSizingMode=!height&&dir==='HORIZONTAL'?'AUTO':'FIXED';n.fills=bg?[paint(bg)]:[];return n;
}
function innerWidth(n){return n.width-(n.paddingLeft||0)-(n.paddingRight||0);}
function pad(n,value,gap=0) { for(const key of ['paddingTop','paddingRight','paddingBottom','paddingLeft']){n[key]=value;const v=variables.find(v=>v.name==='space/'+value);if(v)n.setBoundVariable(key,v);} n.itemSpacing=gap; const s=variables.find(v=>v.name==='space/'+gap);if(s)n.setBoundVariable('itemSpacing',s);return n; }
function round(n,value=16){n.cornerRadius=value;const v=variables.find(v=>v.name==='radius/'+value);if(v)n.setBoundVariable('cornerRadius',v);return n;}
function txt(parent,value,style='Caption',width=null,tone='ink') {
  const n=figma.createText();parent.appendChild(n);created.push(n.id);n.name=style+' / '+value.slice(0,24);
  const key=style==='Strong'?'M&D Mobile V2/Strong':style==='Brand'?'M&D Mobile V2/MobileBrand':'M&D Study/'+style;
  n.textStyleId=sm.get(key).id;n.characters=value;n.fills=[paint(tone)];
  if(width!==null){n.textAutoResize='HEIGHT';n.resize(width,n.height||20);}else n.textAutoResize='WIDTH_AND_HEIGHT';return n;
}
function existing(parent,node){const n=node.clone();parent.appendChild(n);remember(n);return n;}
const sources = await Promise.all(['105:315','56:86','56:111','56:125','56:156','56:150','56:181','56:179','108:3653','356:18','108:1390','108:1492'].map(id=>figma.getNodeByIdAsync(id)));
const [avatarSource,bellSource,checkSource,messageSource,heartSource,adoptionSource,quietSource,primarySource,statusSource,navSource,homeToolbarSource,homeNavSource]=sources;
function icon(parent,source,size=24){const n=source.createInstance();parent.appendChild(n);remember(n);n.resize(size,size);return n;}
function button(parent,label,primary=false,width=120){const n=(primary?primarySource:quietSource).createInstance();parent.appendChild(n);remember(n);n.resize(width,44);const t=n.findAllWithCriteria({types:['TEXT']})[0];t.characters=label;return n;}
const badgeSet = page.children.find(n=>n.name==='M&D / Unread / Badge');
const rowSet = page.children.find(n=>n.name==='M&D / Unread / Message row');
function badge(parent,count,tone='Accent'){
  const master=badgeSet.children.find(n=>n.name==='Tone='+tone);const n=master.createInstance();parent.appendChild(n);remember(n);
  const key=Object.keys(n.componentProperties).find(k=>k.startsWith('Count#'));if(key)n.setProperties({[key]:count>99?'99+':String(count)});if(count===0)n.visible=false;return n;
}
function row(parent,data){
  const master=rowSet.children.find(n=>n.name==='Kind='+(data.kind||'Chat'));const n=master.createInstance();parent.appendChild(n);remember(n);n.resize(innerWidth(parent),88);
  const values={Title:data.title,Preview:data.preview,Meta:data.meta||'',Time:data.time||'刚刚'};
  for(const [name,value] of Object.entries(values)){const key=Object.keys(n.componentProperties).find(k=>k.startsWith(name+'#'));if(key)n.setProperties({[key]:value});}
  const copy=n.findOne(c=>c.name==='Message copy');copy.resize(Math.max(120,n.width-124),68);
  for(const t of copy.findAllWithCriteria({types:['TEXT']})){t.textAutoResize='HEIGHT';t.resize(copy.width,t.height||20);}
  const counter=n.findAllWithCriteria({types:['INSTANCE']}).find(c=>c.name==='Unread counter');
  if(counter){const target=badgeSet.children.find(c=>c.name==='Tone='+(data.muted?'Muted':'Accent'));counter.swapComponent(target);const key=Object.keys(counter.componentProperties).find(k=>k.startsWith('Count#'));if(key)counter.setProperties({[key]:data.count>99?'99+':String(data.count||0)});counter.visible=!!data.count;}
  if(data.muted){const title=n.findAllWithCriteria({types:['TEXT']}).find(t=>t.name==='Row title');if(title)title.fills=[paint('muted')];}
  if(data.icon){const slot=n.findAllWithCriteria({types:['INSTANCE']}).find(c=>c.name==='Notice icon');if(slot)slot.swapComponent(data.icon);}
  return n;
}
function tabs(parent,active,counts=[83,16,8]){
  const rail=pad(frame(parent,'Inbox category tabs','HORIZONTAL',innerWidth(parent),48),0,8);
  ['聊天','互动','通知'].forEach((label,i)=>{const tab=round(pad(frame(rail,'Tab / '+label,'HORIZONTAL',(rail.width-16)/3,48,i===active?'soft':null),8,4),999);tab.primaryAxisAlignItems='CENTER';tab.counterAxisAlignItems='CENTER';txt(tab,label,'Label',null,i===active?'brand':'muted');badge(tab,counts[i],i===active?'Accent':'Muted');});return rail;
}
function phone(parent,key,title='消息',total=107){
  const root=frame(parent,key,'VERTICAL',390,844,'bg');round(root,20);root.clipsContent=true;root.setExplicitVariableModeForCollection('VariableCollectionId:14:6','56:0');root.setExplicitVariableModeForCollection('VariableCollectionId:70:148','70:0');
  existing(root,statusSource);const bar=pad(frame(root,'Toolbar','HORIZONTAL',390,56,'surface'),0,8);bar.paddingLeft=16;bar.paddingRight=16;bar.setBoundVariable('paddingLeft',vm.get('VariableID:14:55'));bar.setBoundVariable('paddingRight',vm.get('VariableID:14:55'));bar.counterAxisAlignItems='CENTER';txt(bar,title,'Section');
  const spacer=frame(bar,'Flexible space','HORIZONTAL',1,1);spacer.layoutSizingHorizontal='FILL';
  const all=button(bar,'全部已读',false,92);all.name='Action / 全部已读';
  const body=pad(frame(root,'Scrollable viewport','VERTICAL',390,676,'bg'),16,12);body.layoutSizingVertical='FILL';body.clipsContent=true;body.overflowDirection='VERTICAL';
  const nav=existing(root,navSource);const messageNav=nav.findAllWithCriteria({types:['INSTANCE']}).find(n=>n.mainComponent&&n.mainComponent.id==='56:227');
  if(messageNav&&total){const b=badge(nav,total);b.layoutPositioning='ABSOLUTE';b.x=messageNav.x+39;b.y=3;b.name='Global unread / '+(total>99?'99+':total);}
  return {root,body,all,bar,nav};
}
function note(parent,title,body,tone='soft'){const card=round(pad(frame(parent,title,'VERTICAL',innerWidth(parent),null,tone),12,4),16);txt(card,title,'Label',card.width-24,'brand');txt(card,body,'Caption',card.width-24,'muted');return card;}
function section(parent,title){return txt(parent,title,'Label',innerWidth(parent),'muted');}
