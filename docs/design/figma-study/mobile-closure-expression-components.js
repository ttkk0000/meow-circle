/**
 * validateCreation
 *
 * Verifies that a set of nodes exist and match expected structural properties.
 * Designed to run immediately after a creation script to catch partial failures
 * before proceeding to the next build phase.
 *
 * Each check specifies a node ID and any combination of expected properties.
 * A check passes when all specified expectations are met; it fails (with a
 * reason string) as soon as any expectation is violated.
 *
 * @param {Array<{
 *   nodeId: string,
 *   expectedChildCount?: number,
 *   expectedName?: string,
 *   expectedType?: NodeType
 * }>} checks
 *   - `nodeId`: The Figma node ID to look up via figma.getNodeById.
 *   - `expectedChildCount`: If set, the node must have exactly this many direct children.
 *     Applies to any node with a `children` property (frames, component sets, etc.).
 *   - `expectedName`: If set, the node's `.name` must exactly match this string.
 *   - `expectedType`: If set, the node's `.type` must exactly match this string.
 * @returns {{
 *   passed: string[],
 *   failed: Array<{nodeId: string, reason: string}>
 * }}
 *   `passed`: Array of nodeIds that passed all checks.
 *   `failed`: Array of objects with the nodeId and a human-readable reason string.
 */
async function validateCreation(checks) {
  const passed = []
  const failed = []

  for (const check of checks) {
    const node = await figma.getNodeByIdAsync(check.nodeId)

    // Node must exist
    if (!node) {
      failed.push({
        nodeId: check.nodeId,
        reason: `Node not found. It may not have been created, or was deleted.`,
      })
      continue
    }

    const reasons = []

    // Type check
    if (check.expectedType !== undefined && node.type !== check.expectedType) {
      reasons.push(`type is "${node.type}", expected "${check.expectedType}"`)
    }

    // Name check
    if (check.expectedName !== undefined && node.name !== check.expectedName) {
      reasons.push(`name is "${node.name}", expected "${check.expectedName}"`)
    }

    // Child count check
    if (check.expectedChildCount !== undefined) {
      if (!('children' in node)) {
        reasons.push(
          `node type "${node.type}" does not have children, but expectedChildCount=${check.expectedChildCount} was specified`,
        )
      } else {
        const actualCount = node.children.length
        if (actualCount !== check.expectedChildCount) {
          reasons.push(`has ${actualCount} children, expected ${check.expectedChildCount}`)
        }
      }
    }

    if (reasons.length > 0) {
      failed.push({
        nodeId: check.nodeId,
        reason: reasons.join('; '),
      })
    } else {
      passed.push(check.nodeId)
    }
  }

  return { passed, failed }
}


const page=await figma.getNodeByIdAsync("56:26");await figma.setCurrentPageAsync(page);
await figma.loadFontAsync({family:"Noto Sans SC",style:"Regular"});await figma.loadFontAsync({family:"Noto Sans SC",style:"Bold"});
const createdNodeIds=[],createdVariableIds=[],components={},opacityVars={},allVars=await figma.variables.getLocalVariablesAsync();const coll=(await figma.variables.getLocalVariableCollectionsAsync()).find(x=>x.id==="VariableCollectionId:141:2");const mode=coll.modes[0].modeId;
const baseX=Math.max(...page.children.map(x=>x.x+x.width),0)+160,baseY=0;
function collect(n){createdNodeIds.push(n.id);if("children"in n)for(const c of n.children)collect(c);}
for(let i=0;i<ASSETS.length;i++){const a=ASSETS[i];const name="M&D Closure/ExpressionAsset/"+a.key;let main=page.findOne(x=>x.type==="COMPONENT"&&x.name===name);if(!main){main=figma.createComponent();page.appendChild(main);main.name=name;main.resize(36,36);main.fills=[];main.x=baseX+i*70;main.y=baseY;const svg=figma.createNodeFromSvg(a.svg);main.appendChild(svg);svg.name="Twemoji / "+a.codepoint;svg.resize(36,36);svg.x=0;svg.y=0;main.description="Twemoji graphics, Twitter, Inc. and contributors. CC BY 4.0. Source: "+a.source+" . Imported unchanged into editable vectors; reusable expression asset, not app implementation.";collect(main);}components[a.key]=main.id;}
for(const key of [...ASSETS.map(x=>x.key),"text"]){const name="expressionOpacity/"+key;let v=allVars.find(x=>x.variableCollectionId===coll.id&&x.name===name);if(!v){v=figma.variables.createVariable(name,coll,"FLOAT");v.scopes=['OPACITY'];v.setValueForMode(mode,key==="hug"?1:0);v.setVariableCodeSyntax("WEB","var(--mnd-prototype-expression-opacity-"+key+")");createdVariableIds.push(v.id);}opacityVars[key]=v.id;}
const review=figma.createFrame();page.appendChild(review);review.name="M&D Closure / Vector expression review";review.resize(1120,280);review.x=baseX;review.y=baseY+120;review.fills=[{type:"SOLID",color:{r:.969,g:.973,b:.98}}];collect(review);
async function text(str,x,y,w,role="Caption"){const n=figma.createText();review.appendChild(n);n.fontName={family:"Noto Sans SC",style:role==="Section"?"Bold":"Regular"};n.characters=str;n.resize(w,40);n.textAutoResize="HEIGHT";n.x=x;n.y=y;n.fills=[{type:"SOLID",color:{r:.125,g:.137,b:.16}}];await n.setTextStyleIdAsync(STYLES[role]);n.textAutoResize="HEIGHT";createdNodeIds.push(n.id);return n;}
await text("共享表情 · 组件资产",24,20,1072,"Section");
for(let i=0;i<ASSETS.length;i++){const a=ASSETS[i],main=await figma.getNodeByIdAsync(components[a.key]);const n=main.createInstance();review.appendChild(n);n.x=36+i*134;n.y=90;n.resize(44,44);collect(n);await text(a.label,24+i*134,148,120);}
await text("Twemoji graphics © Twitter, Inc. and contributors · CC BY 4.0 · Source: github.com/jdecked/twemoji",24,208,1072);
const checks=await validateCreation(Object.values(components).map(nodeId=>({nodeId,expectedType:"COMPONENT",expectedChildCount:1})));
return{createdNodeIds,mutatedNodeIds:[],createdVariableIds,components,opacityVars,reviewId:review.id,checks,graphicLicense:"CC BY 4.0",graphicAttribution:"Twemoji graphics © Twitter, Inc. and contributors"};

