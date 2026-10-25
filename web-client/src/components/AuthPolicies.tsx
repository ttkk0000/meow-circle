export const configuredURL = (value: unknown) => {
  try {
    const u = new URL(String(value));
    return u.protocol === "https:" ? u.href : undefined;
  } catch {
    return undefined;
  }
};
export const termsURL = configuredURL(import.meta.env.VITE_MND_TERMS_URL);
export const privacyURL = configuredURL(import.meta.env.VITE_MND_PRIVACY_URL);
export function LegalLinks() {
  return (
    <div className="legal-links">
      {termsURL ? (
        <a href={termsURL} target="_blank" rel="noopener noreferrer">
          用户协议
        </a>
      ) : (
        <span>用户协议尚未配置</span>
      )}
      {privacyURL ? (
        <a href={privacyURL} target="_blank" rel="noopener noreferrer">
          隐私政策
        </a>
      ) : (
        <span>隐私政策尚未配置</span>
      )}
    </div>
  );
}
