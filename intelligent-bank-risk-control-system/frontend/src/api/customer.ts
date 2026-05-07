import request from './request'
import type { CustomerProfile, CustomerProfileSaveRequest } from './types/customer'
import type { DocumentType, OcrExtractResponse } from './types/ocr'

export const customerApi = {
  getMyProfile() {
    return request.get<any, CustomerProfile>('/customer/profile/me')
  },

  saveMyProfile(data: CustomerProfileSaveRequest) {
    return request.put<any, CustomerProfile>('/customer/profile/me', data)
  },

  extractByOcr(file: File, documentType: DocumentType) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('documentType', documentType)
    return request.post<any, OcrExtractResponse>('/customer/profile/ocr', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
