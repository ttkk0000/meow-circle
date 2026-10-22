// Historical recipe: cfg is injected per page; do not replay without reading the current canvas.
const p=await figma.getNodeByIdAsync(cfg.page);await figma.setCurrentPageAsync(p);
const sections=p.children.filter(n=>n.type==='SECTION');
const beforeIds=p.children.map(n=>n.id).join('|');
const childSignature=sections.map(s=>s.id+':'+s.children.map(n=>n.id+','+n.width+','+n.height).join(';')).join('|');
const before=p.children.map(n=>({id:n.id,x:n.x,y:n.y,w:n.width,h:n.height}));
const oldBottom=Math.max(...p.children.map(n=>n.y+n.height));
const mutatedSections=[], beforeChildren=[];
let rootCount=0;
for(const s of sections){
const ns=s.children.slice().sort((a,b)=>a.y-b.y||a.x-b.x);
beforeChildren.push(...ns.map(n=>n.id+','+n.x+','+n.y));
let y=96,right=0,bottom=0;
for(let i=0;i<ns.length;i+=cfg.cols){
 const row=ns.slice(i,i+cfg.cols);let x=64,maxH=0;
 for(const n of row){n.x=x;n.y=y;x+=n.width+cfg.gapX;right=Math.max(right,n.x+n.width);maxH=Math.max(maxH,n.height);rootCount++;}
 bottom=y+maxH;y=bottom+96;
}
s.resize(Math.max(480,right+64),Math.max(224,bottom+64));mutatedSections.push(s.id);
}
const maxW=Math.max(...sections.map(s=>s.width));let y=120;
for(let i=0;i<sections.length;i+=3){const row=sections.slice(i,i+3);let maxH=0;
for(let j=0;j<row.length;j++){const s=row[j];s.x=1848+j*(maxW+192);s.y=y;maxH=Math.max(maxH,s.height);}y+=maxH+192;}
const after=p.children.map(n=>({id:n.id,x:n.x,y:n.y,w:n.width,h:n.height}));
const unchanged=beforeIds===p.children.map(n=>n.id).join('|')&&childSignature===sections.map(s=>s.id+':'+s.children.map(n=>n.id+','+n.width+','+n.height).join(';')).join('|');
const newBottom=Math.max(...p.children.map(n=>n.y+n.height));
return {page:p.id,mutatedNodeIds:{sections:mutatedSections,childrenBeforeXY:beforeChildren.join(';')},rootCount,before,after,unchangedIdsSizesOrder:unchanged,oldBottom,newBottom};
