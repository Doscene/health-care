/** ASR识别结果 */
export interface AsrResult {
  /** 识别出的文本 */
  text: string;
  /** 置信度 0-1 */
  confidence: number;
  /** 识别语言 */
  language?: string;
  /** 原始响应（调试用） */
  rawResponse?: any;
}

/** ASR Provider 接口 */
export interface AsrProvider {
  /** Provider 名称 */
  readonly name: string;

  /** 识别语音 */
  recognize(audioBase64: string, format?: string): Promise<AsrResult>;

  /** 检查 Provider 是否可用 */
  isAvailable(): Promise<boolean>;
}
