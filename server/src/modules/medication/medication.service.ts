import { Injectable, BadRequestException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class MedicationService {
  constructor(private readonly prisma: PrismaService) {}

  /** 添加用药计划 */
  async createMedication(
    userId: string,
    data: {
      name: string;
      specification: string;
      dosagePerTime: number;
      frequencyPerDay: number;
      timeSlots: string[];
      remindTimes: string[];
      startDate: string;
      endDate?: string;
      notes?: string;
    },
  ) {
    if (!data.name || data.name.trim().length === 0) {
      throw new BadRequestException('药品名称不能为空');
    }
    if (data.dosagePerTime < 1) {
      throw new BadRequestException('每次剂量至少为1');
    }
    if (data.frequencyPerDay < 1 || data.frequencyPerDay > 10) {
      throw new BadRequestException('每日次数应在1-10之间');
    }

    return this.prisma.medication.create({
      data: {
        id: uuidv4(),
        userId,
        name: data.name,
        specification: data.specification ?? '',
        dosagePerTime: data.dosagePerTime,
        frequencyPerDay: data.frequencyPerDay,
        timeSlots: data.timeSlots ?? [],
        remindTimes: data.remindTimes ?? [],
        startDate: new Date(data.startDate),
        endDate: data.endDate ? new Date(data.endDate) : null,
        notes: data.notes ?? null,
      },
    });
  }

  /** 获取用户用药列表 */
  async getMedications(userId: string, status?: string) {
    const where: any = { userId };
    if (status) where.status = status;

    return this.prisma.medication.findMany({
      where,
      orderBy: { startDate: 'desc' },
    });
  }

  /** 获取单个用药详情 */
  async getMedication(userId: string, medicationId: string) {
    const med = await this.prisma.medication.findFirst({
      where: { id: medicationId, userId },
    });
    if (!med) throw new NotFoundException('用药计划不存在');
    return med;
  }

  /** 更新用药计划 */
  async updateMedication(
    userId: string,
    medicationId: string,
    data: {
      name?: string;
      specification?: string;
      dosagePerTime?: number;
      frequencyPerDay?: number;
      timeSlots?: string[];
      remindTimes?: string[];
      endDate?: string;
      notes?: string;
      status?: string;
    },
  ) {
    const med = await this.prisma.medication.findFirst({
      where: { id: medicationId, userId },
    });
    if (!med) throw new NotFoundException('用药计划不存在');

    const updateData: any = {};
    if (data.name !== undefined) updateData.name = data.name;
    if (data.specification !== undefined) updateData.specification = data.specification;
    if (data.dosagePerTime !== undefined) updateData.dosagePerTime = data.dosagePerTime;
    if (data.frequencyPerDay !== undefined) updateData.frequencyPerDay = data.frequencyPerDay;
    if (data.timeSlots !== undefined) updateData.timeSlots = data.timeSlots;
    if (data.remindTimes !== undefined) updateData.remindTimes = data.remindTimes;
    if (data.endDate !== undefined) updateData.endDate = data.endDate ? new Date(data.endDate) : null;
    if (data.notes !== undefined) updateData.notes = data.notes;
    if (data.status !== undefined) updateData.status = data.status;

    return this.prisma.medication.update({
      where: { id: medicationId },
      data: updateData,
    });
  }

  /** 删除用药计划 */
  async deleteMedication(userId: string, medicationId: string) {
    const med = await this.prisma.medication.findFirst({
      where: { id: medicationId, userId },
    });
    if (!med) throw new NotFoundException('用药计划不存在');

    await this.prisma.medicationRecord.deleteMany({ where: { medicationId } });
    await this.prisma.medication.delete({ where: { id: medicationId } });
    return { message: '已删除' };
  }

  /** 确认服药 */
  async confirmMedication(
    userId: string,
    medicationId: string,
    data: { scheduledTime: string; status: string },
  ) {
    const validStatuses = ['taken', 'skipped', 'missed'];
    if (!validStatuses.includes(data.status)) {
      throw new BadRequestException(`无效状态，允许值：${validStatuses.join(', ')}`);
    }

    return this.prisma.medicationRecord.create({
      data: {
        id: uuidv4(),
        medicationId,
        userId,
        scheduledTime: new Date(data.scheduledTime),
        actualTime: data.status === 'taken' ? new Date() : null,
        status: data.status,
        confirmedBy: 'user',
      },
    });
  }

  /** 获取用药记录 */
  async getMedicationRecords(userId: string, medicationId?: string, limit = 30) {
    const where: any = { userId };
    if (medicationId) where.medicationId = medicationId;

    return this.prisma.medicationRecord.findMany({
      where,
      orderBy: { scheduledTime: 'desc' },
      take: limit,
      include: {
        medication: { select: { name: true } },
      },
    });
  }
}
