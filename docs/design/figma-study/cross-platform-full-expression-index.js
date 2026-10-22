// Caller BASE, MOBILE, STATE, CONTEXTS. Native review index, not an app screen.
const page=await figma.getNodeByIdAsync('311:17');await figma.setCurrentPageAsync(page);
await Promise.all([{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'}].map(f=>figma.loadFontAsync(f)));
const vars=await figma.variables.getLocalVariablesAsync(),byId=Object.fromEntries(vars.map(v=>[v.id,v])),createdNodeIds=[],jobs=[];
function cv(v){const x=v.valuesByMode['56:0']??Object.values(v.valuesByMode)[0];return x?.type==='VARIABLE_ALIAS'?cv(byId[x.id]):x;}
function paint(k){const v=byId[BASE.variables[k]],c=cv(v);return figma.variables.setBoundVariableForPaint({type:'SOLID',color:{r:c.r,g:c.g,b:c.b}},'color',v);}
function box(p,name,w,pad=0,gap=16,dir='VERTICAL'){const n=figma.createAutoLayout(dir);p.appendChild(n);n.name=name;n.resize(w,80);n.layoutSizingHorizontal='FIXED';n.layoutSizingVertical='HUG';n.paddingLeft=n.paddingRight=n.paddingTop=n.paddingBottom=pad;n.itemSpacing=gap;n.fills=[paint(p.type==='PAGE'?'background':'surface')];createdNodeIds.push(n.id);return n;}
function tx(p,s,w,role='Body'){const n=figma.createText();p.appendChild(n);n.fontName={family:'Noto Sans SC',style:role==='Title'||role==='Strong'?'Bold':'Regular'};n.characters=s;n.resize(w,24);n.textAutoResize='HEIGHT';n.fills=[paint('ink')];jobs.push(n.setTextStyleIdAsync(MOBILE.styles[role]||BASE.styles[role]));createdNodeIds.push(n.id);return n;}
if(page.children.some(n=>n.name==='M&D / 表情场景索引'))throw Error('Index exists; inspect before changing');
const root=box(page,'M&D / 表情场景索引',1496,40,24);root.x=80;root.y=80;root.setExplicitVariableModeForCollection(BASE.collections.color,'56:0');tx(root,'表情与输入 · 使用场景索引',1416,'Title');tx(root,'16 个场景与连续编辑各有四种对应布局；素材与默认浅色延续手机主版本。',1416);
const main=await figma.getNodeByIdAsync(MOBILE.components.secondary);
for(const c of CONTEXTS){const row=box(root,c.label,1416,16,16,'HORIZONTAL');row.cornerRadius=16;row.counterAxisAlignItems='CENTER';tx(row,c.label,320,'Strong');for(const [key,label]of [['web','Web'],['desktop','Desktop'],['tabletP','平板竖屏'],['tabletL','平板横屏']]){const n=main.createInstance();row.appendChild(n);n.resize(230,48);const prop=Object.keys(n.componentProperties).find(k=>k.split('#')[0]==='Label');if(prop)n.setProperties({[prop]:label});n.name='打开场景 / '+c.label+' / '+label;const id=STATE.roots[key+'/'+c.key];n.reactions=[{trigger:{type:'ON_CLICK'},actions:[{type:'URL',url:'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC/MND?node-id='+id.replace(':','-')}]}];createdNodeIds.push(n.id);}}
tx(root,'本页为评审索引。原稿内保留草稿、预览、取消、连续输入、最近／收藏以及不同发送场景的独立状态。',1416);
await Promise.all(jobs);return {rootId:root.id,createdNodeIds,pageId:page.id,w:root.width,h:root.height,sceneCount:CONTEXTS.length};
