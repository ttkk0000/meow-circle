(async () => {
  await figma.loadFontAsync({ family: "Inter", style: "Medium" }).catch(() => {});
  await figma.loadFontAsync({ family: "Inter", style: "Bold" }).catch(() => {});
  await figma.loadFontAsync({ family: "Inter", style: "Regular" }).catch(() => {});

  const classes = [
    {
      name: "User (用户主体与统计)",
      props: [
        "+ id: int64",
        "+ username: string",
        "+ nickname: string",
        "+ phone: string (加密/非公开)",
        "+ roles: []string [买家/救助人/商家]",
        "+ stats: UserStats (动态/点赞/勋章)"
      ],
      x: 100, y: 100, color: { r: 1, g: 0.95, b: 0.8 } // 暖黄
    },
    {
      name: "Pet / AdoptionPet (单猫稳定档案)",
      props: [
        "+ id: int64",
        "+ owner_id / rescuer_id: int64",
        "+ name: string",
        "+ breed: string",
        "+ health: string (疫苗/绝育说明)",
        "+ status: string [在养/待领养/已领养]"
      ],
      x: 480, y: 100, color: { r: 0.85, g: 0.95, b: 0.85 } // 薄荷绿
    },
    {
      name: "AdoptionApplication (领养申请流)",
      props: [
        "+ id: int64",
        "+ pet_id: int64",
        "+ applicant_id: int64",
        "+ rescuer_id: int64",
        "+ status: ApplicationStatus",
        "+ message: string (养宠条件自述)"
      ],
      x: 480, y: 380, color: { r: 0.9, g: 0.9, b: 1 } // 薰衣草紫
    },
    {
      name: "Post (社区动态与宠友圈)",
      props: [
        "+ id: int64",
        "+ author_id: int64",
        "+ category: PostCategory",
        "+ visibility: [公开/双向好友/私密]",
        "+ media_ids: []int64 (仅照片无视频)"
      ],
      x: 100, y: 380, color: { r: 1, g: 0.9, b: 0.9 } // 珊瑚粉
    }
  ];

  const nodes = [];
  for (const c of classes) {
    const frame = figma.createFrame();
    frame.name = c.name;
    frame.layoutMode = "VERTICAL";
    frame.primaryAxisSizingMode = "AUTO";
    frame.counterAxisSizingMode = "AUTO";
    frame.paddingTop = 16;
    frame.paddingBottom = 16;
    frame.paddingLeft = 20;
    frame.paddingRight = 20;
    frame.itemSpacing = 8;
    frame.cornerRadius = 12;
    frame.fills = [{ type: "SOLID", color: c.color }];
    frame.strokes = [{ type: "SOLID", color: { r: 0.4, g: 0.4, b: 0.4 } }];
    frame.strokeWeight = 1.5;
    frame.x = c.x;
    frame.y = c.y;

    const title = figma.createText();
    title.characters = c.name;
    title.fontSize = 16;
    frame.appendChild(title);

    const divider = figma.createLine();
    divider.resize(240, 0);
    divider.strokes = [{ type: "SOLID", color: { r: 0.6, g: 0.6, b: 0.6 } }];
    frame.appendChild(divider);

    for (const p of c.props) {
      const propText = figma.createText();
      propText.characters = p;
      propText.fontSize = 13;
      frame.appendChild(propText);
    }

    figma.currentPage.appendChild(frame);
    nodes.push(frame);
  }

  figma.viewport.scrollAndZoomIntoView(nodes);
  return { success: true, count: nodes.length };
})();
