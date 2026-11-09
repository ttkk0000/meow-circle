/** Same digit normalization/length as phoneotp; this is no identity/eligibility proof. */
export const validLoginPhone = (value: string) => {
  const digits = value.replace(/[^0-9]/g, "").length;
  return digits >= 10 && digits <= 15;
};
export const validAuthCode = (value: string) => /^[0-9]{6}$/.test(value);

export const otpExpiryMillis = (value: unknown) => typeof value === "string"
  ? Date.parse(value.replace(/(\.\d{3})\d+(?=Z$|[+-]\d\d:\d\d$)/, "$1")) : Number.NaN;
