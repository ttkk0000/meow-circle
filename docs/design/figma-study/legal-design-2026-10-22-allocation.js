const page=await figma.getNodeByIdAsync(PAGE_ID);await figma.setCurrentPageAsync(page);
const name='2026-10-22 · 法律复审设计补齐',created=[],mutated=[],roots={},bounds=[];
let section=page.children.find(n=>n.type==='SECTION'&&n.name===name);
if(!section){section=figma.createSection();page.appendChild(section);section.name=name;const others=page.children.filter(n=>n.id!==section.id);section.x=others.filter(n=>n.x>1600).sort((a,b)=>a.x-b.x)[0]?.x||120;section.y=Math.max(...others.map(n=>n.y+n.height),0)+192;created.push(section.id);}
const pad=48,gap=48,phone=PLATFORMS.length===1&&PLATFORMS[0].key==='phone',tablet=PLATFORMS.length===2,cols=phone?6:tablet?2:2;
let maxX=0,maxY=0;
for(let i=0;i<SCREENS.length;i++)for(const p of PLATFORMS){
let n=section.children.find(n=>n.name==='Legal placeholder / '+p.key+'/'+SCREENS[i].id);
if(!n){n=figma.createFrame();section.appendChild(n);n.name='Legal placeholder / '+p.key+'/'+SCREENS[i].id;n.resize(p.w,p.h);n.placeholder=true;created.push(n.id);}
const x=phone?pad+(i%cols)*(p.w+gap):tablet?pad+(i%2)*2140+(p.key==='tabletL'?882:0):pad+(i%cols)*(p.w+64),y=pad+Math.floor(i/cols)*((tablet?1194:p.h)+80);
n.x=x;n.y=y;maxX=Math.max(maxX,x+p.w);maxY=Math.max(maxY,y+p.h);roots[p.key+'/'+SCREENS[i].id]=n.id;bounds.push({key:SCREENS[i].id,platform:p.key,id:n.id,x,y,w:p.w,h:p.h});}
section.resize(maxX+pad,maxY+pad);mutated.push(section.id);
return{pageId:PAGE_ID,section:{id:section.id,x:section.x,y:section.y,w:section.width,h:section.height},roots,createdNodeIds:created,mutatedNodeIds:mutated,bounds};
