import request from './request'
import type { CustomerAccount } from './types/account'

export const accountApi = {
  myAccounts() {
    return request.get<any, CustomerAccount[]>('/customer/account/my')
  }
}
