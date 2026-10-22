// Read-only Figma structure check. Caller supplies CONFIG, STATE and PLATFORM.
await figma.setCurrentPageAsync(await figma.getNodeByIdAsync(CONFIG.pages[PLATFORM]));
const roots=[],badFonts=[],horizontalOverflow=[],verticalOverflow=[],badTargets=[],fontSet=new Set(),typeCounts={};
let imageCount=0,actionCount=0;
for(const[key,id]of Object.entries(STATE.roots).filter(([k])=>k.startsWith(PLATFORM+'/'))){
  const n=await figma.getNodeByIdAsync(id);if(!n){roots.push({key,id,missing:true});continue;}
  const all=[n,...n.findAll(()=>true)];let texts=0,instances=0,images=0;
  for(const a of all){
    typeCounts[a.type]=(typeCounts[a.type]||0)+1;
    if(a.type==='TEXT'){texts++;fontSet.add(a.fontName.family);if(a.hasMissingFont||!['Noto Sans SC','Manrope'].includes(a.fontName.family))badFonts.push({id:a.id,font:a.fontName});}
    if(a.type==='INSTANCE')instances++;
    if('fills'in a&&Array.isArray(a.fills)&&a.fills.some(f=>f.type==='IMAGE'))images++;
    const p=a.parent;
    // Internal SVG group/path coordinates are not auto-layout box coordinates.
    // Intentional vertical scroll content may exceed its viewport.
    if(a!==n&&a.visible&&p&&'layoutMode'in p&&p.layoutMode!=='NONE'&&a.layoutPositioning!=='ABSOLUTE'&&['FRAME','INSTANCE','TEXT'].includes(a.type)){
      if(a.x+a.width>p.width+2)horizontalOverflow.push({root:key,id:a.id,name:a.name,right:a.x+a.width,parentWidth:p.width});
      if(a.y+a.height>p.height+2&&!['VERTICAL','BOTH'].includes(p.overflowDirection))verticalOverflow.push({root:key,id:a.id,name:a.name,bottom:a.y+a.height,parentHeight:p.height});
    }
    if('reactions'in a&&Array.isArray(a.reactions))for(const r of a.reactions)for(const action of r.actions||[]){if(action.type==='NODE'&&action.navigation==='NAVIGATE'){actionCount++;const dest=await figma.getNodeByIdAsync(action.destinationId);if(!dest||dest.parent.id!==figma.currentPage.id)badTargets.push({source:a.id,destination:action.destinationId});}}
  }
  imageCount+=images;roots.push({key,id,w:n.width,h:n.height,nodes:all.length,texts,instances,images,mode:n.explicitVariableModes,placeholder:n.placeholder});
}
return {platform:PLATFORM,roots,typeCounts,imageCount,actionCount,fonts:[...fontSet],badFonts,horizontalOverflow,verticalOverflow,badTargets};
