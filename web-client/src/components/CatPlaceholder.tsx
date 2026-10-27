/** Decorative family photos only; never substitutes for a real pet's media. */
export function CatPlaceholder({ cat = "garfield" }: { cat?: "garfield" | "golden" }) {
  return <figure className="cat-placeholder"><img src={`/app/assets/cats/${cat}-portrait.jpg`}
    alt={cat === "garfield" ? "戴向日葵围脖的加菲猫，页面配图" : "窗帘旁的金渐层猫，页面配图"}
    width={960} height={960} loading="lazy" /><figcaption>猫咪配图 · 非待领养或寻宠信息</figcaption></figure>;
}
