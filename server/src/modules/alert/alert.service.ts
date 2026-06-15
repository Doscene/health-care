import {
  Injectable,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { RiskEngine } from './risk-engine.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class AlertService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly riskEngine: RiskEngine,
  ) {}

  // ==================== 风险预警 ====================

  /** 获取用户的风险预警列表 */
  async getAlerts(userId: string, status?: string) {
    const where: any = { userId };
    if (status) where.status = status;

    const alerts = await this.prisma.riskAlert.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: 20,
      include: {
        inquiry: { select: { id: true, question: true, options: true, answer: true, answeredAt: true } },
      },
    });

    return alerts.map((a) => ({
      id: a.id,
      level: a.level,
      triggerType: a.triggerType,
      triggerValue: a.triggerValue,
      status: a.status,
      notifiedContacts: a.notifiedContacts,
      createdAt: a.createdAt,
      resolvedAt: a.resolvedAt,
      inquiry: a.inquiry.length > 0 ? a.inquiry[0] : null,
    }));
  }

  /** 更新预警状态（确认/忽略） */
  async updateAlertStatus(userId: string, alertId: string, status: string) {
    const validStatuses = ['acknowledged', 'resolved', 'dismissed'];
    if (!validStatuses.includes(status)) {
      throw new BadRequestException(
        `无效状态，允许值：${validStatuses.join(', ')}`,
      );
    }

    const alert = await this.prisma.riskAlert.findFirst({
      where: { id: alertId, userId },
    });
    if (!alert) throw new NotFoundException('预警不存在');

    return this.prisma.riskAlert.update({
      where: { id: alertId },
      data: {
        status,
        ...(status === 'resolved' ? { resolvedAt: new Date() } : {}),
      },
    });
  }

  /** 处理新录入的健康数据——评估风险并生成告警 */
  async evaluateAndAlert(
    userId: string,
    data: {
      type: 'bp' | 'bg';
      systolic?: number;
      diastolic?: number;
      bgType?: string;
      bgValue?: number;
    },
  ) {
    let result: Awaited<ReturnType<typeof this.riskEngine.evaluateBpRisk>>;

    if (data.type === 'bp' && data.systolic != null && data.diastolic != null) {
      // 单次评估
      result = this.riskEngine.evaluateBpRisk({
        systolic: data.systolic,
        diastolic: data.diastolic,
      });

      // 红色立即告警
      if (result.level === 'red') {
        await this.riskEngine.createAlert(userId, result);
        return { level: result.level, message: result.message };
      }

      // 检查是否需要升级为橙色（仅限黄色以上）
      if (result.level !== 'green') {
        const persistentResult =
          await this.riskEngine.evaluateBpPersistent(userId);
        if (persistentResult) {
          const alertId = await this.riskEngine.createAlert(
            userId,
            persistentResult,
          );
          if (alertId && persistentResult.level === 'orange') {
            await this.riskEngine.createInquiry(
              userId,
              alertId,
              persistentResult.triggerType,
            );
          }
          return { level: persistentResult.level, message: persistentResult.message };
        }
      }

      // 黄色：仅创建告警不通知联系人
      if (result.level === 'yellow') {
        await this.riskEngine.createAlert(userId, result);
      }

      return { level: result.level, message: result.message };
    }

    if (data.type === 'bg' && data.bgType != null && data.bgValue != null) {
      result = this.riskEngine.evaluateBgRisk({
        type: data.bgType,
        value: data.bgValue,
      });

      if (result.level === 'red') {
        await this.riskEngine.createAlert(userId, result);
        return { level: result.level, message: result.message };
      }

      if (result.level !== 'green') {
        const persistentResult =
          await this.riskEngine.evaluateBgPersistent(userId);
        if (persistentResult) {
          const alertId = await this.riskEngine.createAlert(
            userId,
            persistentResult,
          );
          if (alertId && persistentResult.level === 'orange') {
            await this.riskEngine.createInquiry(
              userId,
              alertId,
              persistentResult.triggerType,
            );
          }
          return { level: persistentResult.level, message: persistentResult.message };
        }
      }

      if (result.level === 'yellow') {
        await this.riskEngine.createAlert(userId, result);
      }

      return { level: result.level, message: result.message };
    }

    return { level: 'green' as const, message: '数据正常' };
  }

  // ==================== 问询卡片 ====================

  /** 提交问询回答 */
  async submitInquiry(userId: string, alertId: string, answer: string) {
    const alert = await this.prisma.riskAlert.findFirst({
      where: { id: alertId, userId },
      include: { inquiry: true },
    });
    if (!alert) throw new NotFoundException('告警不存在');

    const inquiry = alert.inquiry[0];
    if (!inquiry) throw new NotFoundException('问询卡片不存在');
    if (inquiry.answer) throw new BadRequestException('已提交过回答');

    return this.prisma.riskInquiry.update({
      where: { id: inquiry.id },
      data: { answer, answeredAt: new Date() },
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
    const count = await this.prisma.emergencyContact.count({
      where: { userId },
    });
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
    data: {
      name?: string;
      phone?: string;
      relation?: string;
      priority?: number;
    },
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

    await this.prisma.emergencyContact.delete({
      where: { id: contactId },
    });
    return { message: '已删除' };
  }

  /** 设为互为紧急联系人 */
  async setMutualContact(userId: string, targetUserId: string) {
    if (userId === targetUserId) {
      throw new BadRequestException('不能设置自己为联系人');
    }

    const selfUser = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { name: true, phone: true },
    });
    const targetUser = await this.prisma.user.findUnique({
      where: { id: targetUserId },
      select: { name: true, phone: true },
    });
    if (!selfUser || !targetUser) {
      throw new NotFoundException('用户不存在');
    }

    // 检查双方是否已存在
    const selfCount = await this.prisma.emergencyContact.count({
      where: { userId, phone: targetUser.phone },
    });
    const targetCount = await this.prisma.emergencyContact.count({
      where: { userId: targetUserId, phone: selfUser.phone },
    });

    if (selfCount === 0) {
      const count = await this.prisma.emergencyContact.count({
        where: { userId },
      });
      if (count < 5) {
        await this.prisma.emergencyContact.create({
          data: {
            id: uuidv4(),
            userId,
            name: targetUser.name,
            phone: targetUser.phone,
            relation: '家人',
            priority: count,
            isMutual: true,
          },
        });
      }
    }

    if (targetCount === 0) {
      const count = await this.prisma.emergencyContact.count({
        where: { userId: targetUserId },
      });
      if (count < 5) {
        await this.prisma.emergencyContact.create({
          data: {
            id: uuidv4(),
            userId,
            name: selfUser.name,
            phone: selfUser.phone,
            relation: '家人',
            priority: count,
            isMutual: true,
          },
        });
      }
    }

    // 更新已有记录为互为联系人
    await Promise.all([
      this.prisma.emergencyContact.updateMany({
        where: { userId, phone: targetUser.phone },
        data: { isMutual: true },
      }),
      this.prisma.emergencyContact.updateMany({
        where: { userId: targetUserId, phone: selfUser.phone },
        data: { isMutual: true },
      }),
    ]);

    return { message: '已设置为互为紧急联系人' };
  }

  // ==================== 急救包管理 ====================

  /** 获取急救包物品列表 */
  async getFirstAidKit(userId: string) {
    return this.prisma.firstAidKit.findMany({
      where: { userId },
      orderBy: { expireDate: { sort: 'asc', nulls: 'last' } },
    });
  }

  /** 添加急救包物品 */
  async addFirstAidItem(
    userId: string,
    data: {
      name: string;
      type: string;
      quantity?: number;
      expireDate?: string;
      notes?: string;
    },
  ) {
    if (!data.name || data.name.trim().length === 0) {
      throw new BadRequestException('物品名称不能为空');
    }
    const validTypes = ['medicine', 'supply'];
    if (!validTypes.includes(data.type)) {
      throw new BadRequestException(
        `无效类型，允许值：${validTypes.join(', ')}`,
      );
    }

    return this.prisma.firstAidKit.create({
      data: {
        id: uuidv4(),
        userId,
        name: data.name,
        type: data.type,
        quantity: data.quantity ?? 1,
        expireDate: data.expireDate ? new Date(data.expireDate) : null,
        notes: data.notes ?? null,
      },
    });
  }

  /** 更新急救包物品 */
  async updateFirstAidItem(
    userId: string,
    itemId: string,
    data: {
      name?: string;
      type?: string;
      quantity?: number;
      expireDate?: string;
      notes?: string;
    },
  ) {
    const item = await this.prisma.firstAidKit.findFirst({
      where: { id: itemId, userId },
    });
    if (!item) throw new NotFoundException('物品不存在');

    const updateData: any = {};
    if (data.name !== undefined) updateData.name = data.name;
    if (data.type !== undefined) updateData.type = data.type;
    if (data.quantity !== undefined) updateData.quantity = data.quantity;
    if (data.expireDate !== undefined)
      updateData.expireDate = new Date(data.expireDate);
    if (data.notes !== undefined) updateData.notes = data.notes;

    return this.prisma.firstAidKit.update({
      where: { id: itemId },
      data: updateData,
    });
  }

  /** 删除急救包物品 */
  async deleteFirstAidItem(userId: string, itemId: string) {
    const item = await this.prisma.firstAidKit.findFirst({
      where: { id: itemId, userId },
    });
    if (!item) throw new NotFoundException('物品不存在');

    await this.prisma.firstAidKit.delete({ where: { id: itemId } });
    return { message: '已删除' };
  }

  /** 获取急救指南列表 */
  async getEmergencyGuides(type?: string) {
    const where: any = {};
    if (type) where.type = type;

    return this.prisma.firstAidGuide.findMany({
      where,
      orderBy: { order: 'asc' },
    });
  }
}
