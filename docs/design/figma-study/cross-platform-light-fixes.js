// Final Figma patch recipe, only for this task's exact roots. CONFIG/STATE/PLATFORM supplied.
await figma.setCurrentPageAsync(await figma.getNodeByIdAsync(CONFIG.pages[PLATFORM]));
await Promise.all([{family:'Noto Sans SC',style:'Regular'},{family:'Noto Sans SC',style:'Medium'},{family:'Noto Sans SC',style:'Bold'}].map(f=>figma.loadFontAsync(f)));
const mutatedNodeIds=[];
for(const rootId of Object.entries(STATE.roots).filter(([k])=>k.startsWith(PLATFORM+'/')).map(([,id])=>id)){
  const root=await figma.getNodeByIdAsync(rootId);
  for(const n of root.findAllWithCriteria({types:['INSTANCE']})){
    if(n.name==='Instance / chip'||n.name==='Instance / chipSelected'){
      const t=n.findAllWithCriteria({types:['TEXT']})[0];if(t){const w=Math.max(80,t.characters.length*16+40);if(n.width<w){n.resize(w,n.height);mutatedNodeIds.push(n.id);}}
    }
    if(PLATFORM==='tablet'&&n.name==='Instance / quiet → lost'&&n.findAllWithCriteria({types:['TEXT']}).some(t=>t.characters==='互助')){
      const reactions=n.reactions;n.swapComponent(await figma.getNodeByIdAsync(STATE.components.navDefaultCross));const props={};
      for(const k of Object.keys(n.componentProperties)){if(k.split('#')[0]==='Label')props[k]='';if(k.split('#')[0]==='Icon')props[k]='56:107';}
      n.setProperties(props);n.resize(56,52);n.name='Navigation / 走失互助';n.reactions=reactions;for(const t of n.findAllWithCriteria({types:['TEXT']})){t.visible=false;mutatedNodeIds.push(t.id);}mutatedNodeIds.push(n.id);
    }
    if(PLATFORM==='tablet'&&n.name.includes('→ ')){
      const to=n.name.split('→ ').pop(),suffix=root.width>=1000?'L':'P';let id=STATE.roots['tablet/'+to+suffix];
      if(to==='detail'&&suffix==='L')id=STATE.roots['tablet/adoptL'];
      if(id&&id!==root.id){n.reactions=[{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:id,navigation:'NAVIGATE',transition:null}]}];mutatedNodeIds.push(n.id);}
    }
  }
}
return {platform:PLATFORM,mutatedNodeIds};
