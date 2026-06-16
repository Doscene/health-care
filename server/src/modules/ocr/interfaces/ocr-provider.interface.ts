/** OCR识别结果 */
export interface OcrResult {
  /** 识别出的文本行 */
  lines: OcrLine[];
  /** 整体置信度 0-1 */
  confidence: number;
  /** 原始响应（调试用） */
  rawResponse?: any;
}

/** OCR文本行 */
export interface OcrLine {
  /** 文本内容 */
  text: string;
  /** 置信度 0-1 */
  confidence: number;
  /** 文本框位置 */
  position?: {
    left: number;
    top: number;
    width: number;
    height: number;
  };
}

/** OCR Provider 接口 */
export interface OcrProvider {
  /** Provider 名称 */
  readonly name: string;

  /** 识别图片文字 */
  recognize(imageBase64: string): Promise<OcrResult>;

  /** 检查 Provider 是否可用 */
  isAvailable(): Promise<boolean>;
}
