import { Injectable, BadRequestException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class AlertService {
  constructor(private readonly prisma: PrismaService) {}

  // ==================== 风险预警 ====================

  /** 获取用户的风险预警列表 */
  async getAlerts(userId: string, status?: string) {
    const where: any = { userId };
    if (status) where.status = status;

    return this.prisma.riskAlert.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: 20,
    });
  }

  /** 更新预警状态（确认/忽略） */
  async updateAlertStatus(userId: string, alertId: string, status: string) {
    const validStatuses = ['acknowledged', 'resolved', 'dismissed'];
    if (!validStatuses.includes(status)) {
      throw new BadRequestException(`无效状态，允许值：${validStatuses.join(', ')}`);
    }

    const alert = await this.prisma.riskAlert.findFirst({
      where: { id: alertId, userId },
    });
    if (!alert) throw new NotFoundException('预警不存在');

    return this.prisma.riskAlert.update({
      where: { id: alertId },
      data: { status },
    });
  }

  // ==================== 紧急联系人 ====================

  /** 添加紧急联系人 */
  async addEmergencyContact(
    userId: string,
    data: { name: string; phone: string; relation: string; priority?: number },
  ) {
    if (!data.name || data.name.trim().length === 0) {
      throw new BadRequestException('联系人姓名不能为空');
    }
    if (!data.phone || !data.phone.match(/^1[3-9]\d{9}$/)) {
      throw new BadRequestException('请输入正确的手机号');
    }

    // 检查是否超过上限（最多5个）
    const count = await this.prisma.emergencyContact.count({ where: { userId } });
    if (count >= 5) {
      throw new BadRequestException('紧急联系人最多5个');
    }

    return this.prisma.emergencyContact.create({
      data: {
        id: uuidv4(),
        userId,
        name: data.name,
        phone: data.phone,
        relation: data.relation,
        priority: data.priority ?? count,
      },
    });
  }

  /** 获取紧急联系人列表 */
  async getEmergencyContacts(userId: string) {
    return this.prisma.emergencyContact.findMany({
      where: { userId },
      orderBy: { priority: 'asc' },
    });
  }

  /** 更新紧急联系人 */
  async updateEmergencyContact(
    userId: string,
    contactId: string,
    data: { name?: string; phone?: string; relation?: string; priority?: number },
  ) {
    const contact = await this.prisma.emergencyContact.findFirst({
      where: { id: contactId, userId },
    });
    if (!contact) throw new NotFoundException('联系人不存在');

    if (data.phone && !data.phone.match(/^1[3-9]\d{9}$/)) {
      throw new BadRequestException('请输入正确的手机号');
    }

    const updateData: any = {};
    if (data.name !== undefined) updateData.name = data.name;
    if (data.phone !== undefined) updateData.phone = data.phone;
    if (data.relation !== undefined) updateData.relation = data.relation;
    if (data.priority !== undefined) updateData.priority = data.priority;

    return this.prisma.emergencyContact.update({
      where: { id: contactId },
      data: updateData,
    });
  }

  /** 删除紧急联系人 */
  async deleteEmergencyContact(userId: string, contactId: string) {
    const contact = await this.prisma.emergencyContact.findFirst({
      where: { id: contactId, userId },
    });
    if (!contact) throw new NotFoundException('联系人不存在');

    await this.prisma.emergencyContact.delete({ where: { id: contactId } });
    return { message: '已删除' };
  }
}
