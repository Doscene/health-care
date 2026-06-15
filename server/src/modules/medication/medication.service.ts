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

  /** 检测多药冲突 */
  async checkConflicts(userId: string, medicationIds: string[]) {
    if (medicationIds.length < 2) return { conflicts: [] };

    const meds = await this.prisma.medication.findMany({
      where: { id: { in: medicationIds }, userId },
      select: { id: true, name: true },
    });

    if (meds.length < 2) return { conflicts: [] };

    const medNames = meds.map((m) => m.name);
    const conflicts = await this.prisma.medicationConflict.findMany({
      where: {
        OR: medNames.flatMap((name) => [
          { drugA: { contains: name } },
          { drugB: { contains: name } },
        ]),
      },
    });

    // 匹配当前用药
    const matched = conflicts.filter((c) => {
      const matchA = medNames.some((n) => c.drugA.includes(n) || n.includes(c.drugA));
      const matchB = medNames.some((n) => c.drugB.includes(n) || n.includes(c.drugB));
      return matchA && matchB && c.drugA !== c.drugB;
    });

    return {
      conflicts: matched.map((c) => ({
        drugA: c.drugA,
        drugB: c.drugB,
        severity: c.severity,
        description: c.description,
      })),
    };
  }

  /** 获取服药日历（月级别聚合） */
  async getCalendar(
    userId: string,
    year: number,
    month: number,
    medicationId?: string,
  ) {
    const startDate = new Date(year, month - 1, 1);
    const endDate = new Date(year, month, 0, 23, 59, 59);

    const where: any = {
      userId,
      scheduledTime: { gte: startDate, lte: endDate },
    };
    if (medicationId) where.medicationId = medicationId;

    const records = await this.prisma.medicationRecord.findMany({
      where,
      orderBy: { scheduledTime: 'asc' },
      include: {
        medication: { select: { name: true } },
      },
    });

    // 按天分组
    const calendarMap = new Map<string, typeof records>();
    for (const r of records) {
      const day = r.scheduledTime.toISOString().split('T')[0];
      if (!calendarMap.has(day)) calendarMap.set(day, []);
      calendarMap.get(day)!.push(r);
    }

    const days = Array.from(calendarMap.entries()).map(([date, recs]) => {
      const taken = recs.filter((r) => r.status === 'taken').length;
      const total = recs.length;
      const status = taken === total ? 'complete' : taken > 0 ? 'partial' : 'missed';
      return {
        date,
        status,
        taken,
        missed: total - taken,
        total,
        medications: [...new Set(recs.map((r) => r.medication.name))],
      };
    });

    return {
      year,
      month,
      totalRecords: records.length,
      adherenceRate:
        records.length > 0
          ? Math.round(
              (records.filter((r) => r.status === 'taken').length /
                records.length) *
                100,
            ) / 100
          : 0,
      days,
    };
  }

  /** 用药依从性统计 */
  async getAdherence(userId: string, days: number = 30) {
    const since = new Date(Date.now() - days * 24 * 60 * 60 * 1000);

    const records = await this.prisma.medicationRecord.findMany({
      where: { userId, scheduledTime: { gte: since } },
      select: { status: true, scheduledTime: true },
    });

    const total = records.length;
    const taken = records.filter((r) => r.status === 'taken').length;
    const missedOrSkipped = records.filter((r) =>
      ['missed', 'skipped'].includes(r.status),
    ).length;

    // 按天统计趋势
    const dailyMap = new Map<string, { taken: number; total: number }>();
    for (const r of records) {
      const day = r.scheduledTime.toISOString().split('T')[0];
      if (!dailyMap.has(day)) dailyMap.set(day, { taken: 0, total: 0 });
      const entry = dailyMap.get(day)!;
      entry.total++;
      if (r.status === 'taken') entry.taken++;
    }

    const dailyTrend = Array.from(dailyMap.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, data]) => ({
        date,
        rate: data.total > 0 ? Math.round((data.taken / data.total) * 100) : 0,
        taken: data.taken,
        total: data.total,
      }));

    return {
      periodDays: days,
      overallRate: total > 0 ? Math.round((taken / total) * 100) : 0,
      totalRecords: total,
      taken,
      missed: missedOrSkipped,
      dailyTrend,
    };
  }
}
