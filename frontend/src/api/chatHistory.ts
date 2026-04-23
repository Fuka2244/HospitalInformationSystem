import { get, post, del } from './request'
import type { ChatMessageDto } from '@/types'

/** 获取指定类型的聊天历史 */
export function getChatHistory(chatType: string) {
  return get<ChatMessageDto[]>('/chat-history/list', { chatType })
}

/** 保存一条聊天消息 */
export function saveChatMessage(data: { chatType: string; role: string; content: string }) {
  return post('/chat-history/save', data)
}

/** 批量保存聊天消息 */
export function saveChatMessages(data: { chatType: string; messages: { role: string; content: string }[] }) {
  return post('/chat-history/save-batch', data)
}

/** 清除指定类型的聊天历史 */
export function clearChatHistory(chatType: string) {
  return del('/chat-history/clear?chatType=' + encodeURIComponent(chatType))
}
