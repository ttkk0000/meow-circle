// Inputs: ASSETS (official Noto SVGs), LEGACY (preserved master IDs).
const page=await figma.getNodeByIdAsync("56:26");await figma.setCurrentPageAsync(page);
const createdNodeIds=[],mutatedNodeIds=[],removedNodeIds=[],components={},opacityVars={};
const opacityTemplate=await figma.variables.getVariableByIdAsync("VariableID:160:889");
const collection=await figma.variables.getVariableCollectionByIdAsync(opacityTemplate.variableCollectionId);
const all=await figma.variables.getLocalVariablesAsync();
function track(n){createdNodeIds.push(n.id);if("children"in n)for(const c of n.children)track(c);}
for(const asset of ASSETS){const name="M&D Closure/ExpressionAsset/"+asset.key;let main=LEGACY[asset.key]?await figma.getNodeByIdAsync(LEGACY[asset.key]):page.findOne(n=>n.type==="COMPONENT"&&n.name===name);
if(!main){main=figma.createComponent();page.appendChild(main);main.name=name;main.resize(36,36);main.fills=[];main.x=13500+(asset.index%16)*64;main.y=800+Math.floor(asset.index/16)*64;createdNodeIds.push(main.id);}else{for(const child of [...main.children]){removedNodeIds.push(child.id);child.remove();}mutatedNodeIds.push(main.id);}
main.description="Google Noto Emoji / "+asset.label+" · Official Google Fonts SVG · retain source attribution. Source: "+asset.source;
main.clipsContent=true;const svg=figma.createNodeFromSvg(asset.svg);main.appendChild(svg);svg.name="Noto / "+asset.codepoint;svg.rescale(36/svg.width);svg.x=0;svg.y=0;svg.constraints={horizontal:"SCALE",vertical:"SCALE"};track(svg);components[asset.key]=main.id;
let variable=all.find(v=>v.name==="expression/show/"+asset.key)||all.find(v=>v.id===LEGACY.opacity?.[asset.key]);
if(!variable){variable=figma.variables.createVariable("expression/show/"+asset.key,collection,"FLOAT");for(const mode of collection.modes)variable.setValueForMode(mode.modeId,0);variable.scopes=["OPACITY"];variable.setVariableCodeSyntax("WEB","var(--mnd-expression-"+asset.key+")");}
opacityVars[asset.key]=variable.id;}
return{createdNodeIds,mutatedNodeIds,removedNodeIds,components,opacityVars,count:ASSETS.length};
