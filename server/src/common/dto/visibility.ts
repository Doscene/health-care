/**
 * 家庭成员之间的数据可见性配置（存于 FamilyMember.visibility Json 字段）。
 *
 * 表示「我」对其他家庭成员开放的指标可见级别：
 *   - all     → 完整数据（指标列表/趋势/详情）
 *   - summary → 仅摘要（状态标签 + 平均值），不返回原始记录
 *   - none    → 完全不可见，接口返回空数据 + 提示
 *
 * 默认所有指标对家庭成员均为 summary。
 */

export type VisibilityLevel = 'all' | 'summary' | 'none';

export type VisibilityMetric = 'bp' | 'bg' | 'medication' | 'diet';

export interface VisibilityConfig {
  bp: VisibilityLevel;
  bg: VisibilityLevel;
  medication: VisibilityLevel;
  diet: VisibilityLevel;
}

export const DEFAULT_VISIBILITY: VisibilityConfig = {
  bp: 'summary',
  bg: 'summary',
  medication: 'summary',
  diet: 'summary',
};

/**
 * 解析数据库 Json 字段并填充缺省值，避免后续读取时为 undefined。
 */
export function normalizeVisibility(raw: unknown): VisibilityConfig {
  if (!raw || typeof raw !== 'object') return { ...DEFAULT_VISIBILITY };
  const r = raw as Record<string, unknown>;
  const pick = (key: VisibilityMetric): VisibilityLevel => {
    const value = r[key];
    if (value === 'all' || value === 'summary' || value === 'none') return value;
    return DEFAULT_VISIBILITY[key];
  };
  return {
    bp: pick('bp'),
    bg: pick('bg'),
    medication: pick('medication'),
    diet: pick('diet'),
  };
}

/**
 * 当查看者就是被查看者本人时，自然拥有完整可见性。
 */
export function selfVisibility(): VisibilityConfig {
  return { bp: 'all', bg: 'all', medication: 'all', diet: 'all' };
}

/**
 * 按某指标的可见性级别裁剪数据，返回的字段需进一步交给 Controller 直出。
 */
export interface VisibilityResult<T> {
  level: VisibilityLevel;
  data: T | null;
  hint?: string;
}

export function gateData<T>(
  level: VisibilityLevel,
  full: T,
  summaryFallback?: T,
): VisibilityResult<T> {
  if (level === 'none') {
    return { level, data: null, hint: '该成员未授权查看此项数据' };
  }
  if (level === 'summary' && summaryFallback !== undefined) {
    return { level, data: summaryFallback };
  }
  return { level, data: full };
}
