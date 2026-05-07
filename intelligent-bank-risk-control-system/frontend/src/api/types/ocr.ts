export type DocumentType = 'ID_CARD' | 'BUSINESS_LICENSE'

export interface OcrExtractResponse {
  documentType: DocumentType
  realName?: string
  idCardNo?: string
  address?: string
  phone?: string
  confidenceHint?: string
  rawTextHint?: string
}
