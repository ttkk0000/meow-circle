// Historical recipe with injected cfg; do not replay without reading the current file.
const p=await figma.getNodeByIdAsync(cfg.page);await figma.setCurrentPageAsync(p);
const sections=p.children.filter(n=>n.type==='SECTION');
const before=p.children.map(n=>({id:n.id,x:n.x,y:n.y,w:n.width,h:n.height}));
const beforeIds=p.children.map(n=>n.id).join('|');
const signature=sections.map(s=>s.id+':'+s.children.map(n=>n.id+','+n.width+','+n.height).join(';')).join('|');
const beforeChildren=[],mutatedSections=[];
for(const s of sections){const cs=s.children.slice().sort((a,b)=>a.y-b.y||a.x-b.x);
beforeChildren.push(...cs.map(n=>n.id+','+n.x+','+n.y));
let right=0,bottom=0;
if(cfg.kind==='archive'){
 const rows=[];for(const n of cs){const last=rows[rows.length-1];if(!last||Math.abs(last.oldY-n.y)>1)rows.push({oldY:n.y,ns:[n]});else last.ns.push(n);}
 let y=96;for(const row of rows){let x=64,h=0;for(const n of row.ns){n.x=x;n.y=y;x+=n.width+96;right=Math.max(right,n.x+n.width);h=Math.max(h,n.height);}bottom=y+h;y=bottom+128;}
}else{
 let x=64,y=96,rowH=0;const limit=s.id==='421:11'?4800:3600;const minCell=s.id==='421:8'?160:0;
 for(const n of cs){const cw=Math.max(n.width,minCell);if(x>64&&x+cw>limit+64){x=64;y+=rowH+96;rowH=0;}
 n.x=x;n.y=y;x+=cw+96;right=Math.max(right,n.x+n.width);bottom=Math.max(bottom,n.y+n.height);rowH=Math.max(rowH,n.height,s.id==='421:8'?48:0);}
}
s.resize(Math.max(480,right+64),Math.max(224,bottom+64));mutatedSections.push(s.id);}
let x=120,y=120,rowH=0;for(const s of sections){if(x>120&&x+s.width>cfg.canvasWidth+120){x=120;y+=rowH+192;rowH=0;}s.x=x;s.y=y;x+=s.width+192;rowH=Math.max(rowH,s.height);}
const after=p.children.map(n=>({id:n.id,x:n.x,y:n.y,w:n.width,h:n.height}));
return {page:p.id,mutatedNodeIds:{sections:mutatedSections,childrenBeforeXY:beforeChildren.join(';')},before,after,unchangedIdsSizesOrder:beforeIds===p.children.map(n=>n.id).join('|')&&signature===sections.map(s=>s.id+':'+s.children.map(n=>n.id+','+n.width+','+n.height).join(';')).join('|')};
