import {
  Injectable,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { AlertService } from '../alert/alert.service.js';

/** 血压记录创建参数 */
interface CreateBpRecordDto {
  systolic: number;
  diastolic: number;
  heartRate?: number;
  inputMethod: string;
  source?: string;
  recordedAt?: string;
}

/** 血糖记录创建参数 */
interface CreateBgRecordDto {
  type: string;
  value: number;
  inputMethod: string;
  source?: string;
  recordedAt?: string;
}

/** 分页查询参数 */
interface RecordQueryDto {
  startDate?: string;
  endDate?: string;
  page?: number;
  pageSize?: number;
}

@Injectable()
export class HealthMetricsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly alertService: AlertService,
  ) {}

  // ==================== 血压记录 ====================

  /** 创建血压记录 */
  async createBpRecord(userId: string, dto: CreateBpRecordDto) {
    // 校验：收缩压必须大于舒张压
    if (dto.systolic <= dto.diastolic) {
      throw new BadRequestException('收缩压必须大于舒张压');
    }

    // 校验：异常值拦截
    if (dto.systolic < 60 || dto.systolic > 300) {
      throw new BadRequestException('收缩压数值异常（正常范围60-300）');
    }
    if (dto.diastolic < 30 || dto.diastolic > 200) {
      throw new BadRequestException('舒张压数值异常（正常范围30-200）');
    }
    if (
      dto.heartRate !== undefined &&
      (dto.heartRate < 30 || dto.heartRate > 250)
    ) {
      throw new BadRequestException('心率数值异常（正常范围30-250）');
    }

    const record = await this.prisma.bloodPressureRecord.create({
      data: {
        userId,
        systolic: dto.systolic,
        diastolic: dto.diastolic,
        heartRate: dto.heartRate,
        inputMethod: dto.inputMethod,
        source: dto.source,
        recordedAt: dto.recordedAt ? new Date(dto.recordedAt) : new Date(),
      },
    });

    // 触发风险评估
    await this.alertService.evaluateAndAlert(userId, {
      type: 'bp',
      systolic: dto.systolic,
      diastolic: dto.diastolic,
    });

    return record;
  }

  /** 查询血压记录列表 */
  async getBpRecords(userId: string, query: RecordQueryDto) {
    const { startDate, endDate, page = 1, pageSize = 20 } = query;

    const where: any = { userId };

    if (startDate || endDate) {
      where.recordedAt = {};
      if (startDate) where.recordedAt.gte = new Date(startDate);
      if (endDate) where.recordedAt.lte = new Date(endDate + 'T23:59:59');
    }

    const [records, total] = await Promise.all([
      this.prisma.bloodPressureRecord.findMany({
        where,
        orderBy: { recordedAt: 'desc' },
        skip: (page - 1) * pageSize,
        take: pageSize,
      }),
      this.prisma.bloodPressureRecord.count({ where }),
    ]);

    return {
      list: records,
      total,
      page,
      pageSize,
      totalPages: Math.ceil(total / pageSize),
    };
  }

  /** 获取血压记录详情 */
  async getBpRecord(userId: string, recordId: string) {
    const record = await this.prisma.bloodPressureRecord.findFirst({
      where: { id: recordId, userId },
    });

    if (!record) {
      throw new NotFoundException('血压记录不存在');
    }

    return record;
  }

  /** 更新血压记录 */
  async updateBpRecord(
    userId: string,
    recordId: string,
    dto: Partial<CreateBpRecordDto>,
  ) {
    const existing = await this.prisma.bloodPressureRecord.findFirst({
      where: { id: recordId, userId },
    });

    if (!existing) {
      throw new NotFoundException('血压记录不存在');
    }

    // 校验：如果提供了新的收缩压和舒张压
    const systolic = dto.systolic ?? existing.systolic;
    const diastolic = dto.diastolic ?? existing.diastolic;

    if (systolic <= diastolic) {
      throw new BadRequestException('收缩压必须大于舒张压');
    }

    return this.prisma.bloodPressureRecord.update({
      where: { id: recordId },
      data: {
        systolic: dto.systolic,
        diastolic: dto.diastolic,
        heartRate: dto.heartRate,
        inputMethod: dto.inputMethod,
        source: dto.source,
        recordedAt: dto.recordedAt ? new Date(dto.recordedAt) : undefined,
      },
    });
  }

  /** 删除血压记录 */
  async deleteBpRecord(userId: string, recordId: string) {
    const existing = await this.prisma.bloodPressureRecord.findFirst({
      where: { id: recordId, userId },
    });

    if (!existing) {
      throw new NotFoundException('血压记录不存在');
    }

    await this.prisma.bloodPressureRecord.delete({
      where: { id: recordId },
    });

    return { deleted: true };
  }

  // ==================== 血糖记录 ====================

  /** 创建血糖记录 */
  async createBgRecord(userId: string, dto: CreateBgRecordDto) {
    // 校验血糖类型
    const validTypes = [
      'fasting',
      'before_meal',
      'after_meal',
      'random',
      'bedtime',
    ];
    if (!validTypes.includes(dto.type)) {
      throw new BadRequestException(
        `血糖类型无效，可选值：${validTypes.join(', ')}`,
      );
    }

    // 校验血糖值
    if (dto.value < 1.0 || dto.value > 35.0) {
      throw new BadRequestException('血糖数值异常（正常范围1.0-35.0 mmol/L）');
    }

    const record = await this.prisma.bloodSugarRecord.create({
      data: {
        userId,
        type: dto.type,
        value: dto.value,
        inputMethod: dto.inputMethod,
        source: dto.source,
        recordedAt: dto.recordedAt ? new Date(dto.recordedAt) : new Date(),
      },
    });

    // 触发风险评估
    await this.alertService.evaluateAndAlert(userId, {
      type: 'bg',
      bgType: dto.type,
      bgValue: dto.value,
    });

    return record;
  }

  /** 查询血糖记录列表 */
  async getBgRecords(
    userId: string,
    query: RecordQueryDto & { type?: string },
  ) {
    const { startDate, endDate, type, page = 1, pageSize = 20 } = query;

    const where: any = { userId };

    if (type) {
      where.type = type;
    }

    if (startDate || endDate) {
      where.recordedAt = {};
      if (startDate) where.recordedAt.gte = new Date(startDate);
      if (endDate) where.recordedAt.lte = new Date(endDate + 'T23:59:59');
    }

    const [records, total] = await Promise.all([
      this.prisma.bloodSugarRecord.findMany({
        where,
        orderBy: { recordedAt: 'desc' },
        skip: (page - 1) * pageSize,
        take: pageSize,
      }),
      this.prisma.bloodSugarRecord.count({ where }),
    ]);

    return {
      list: records,
      total,
      page,
      pageSize,
      totalPages: Math.ceil(total / pageSize),
    };
  }

  /** 获取血糖记录详情 */
  async getBgRecord(userId: string, recordId: string) {
    const record = await this.prisma.bloodSugarRecord.findFirst({
      where: { id: recordId, userId },
    });

    if (!record) {
      throw new NotFoundException('血糖记录不存在');
    }

    return record;
  }

  /** 更新血糖记录 */
  async updateBgRecord(
    userId: string,
    recordId: string,
    dto: Partial<CreateBgRecordDto>,
  ) {
    const existing = await this.prisma.bloodSugarRecord.findFirst({
      where: { id: recordId, userId },
    });

    if (!existing) {
      throw new NotFoundException('血糖记录不存在');
    }

    // 校验类型
    if (dto.type) {
      const validTypes = [
        'fasting',
        'before_meal',
        'after_meal',
        'random',
        'bedtime',
      ];
      if (!validTypes.includes(dto.type)) {
        throw new BadRequestException(
          `血糖类型无效，可选值：${validTypes.join(', ')}`,
        );
      }
    }

    // 校验值
    if (dto.value !== undefined && (dto.value < 1.0 || dto.value > 35.0)) {
      throw new BadRequestException('血糖数值异常（正常范围1.0-35.0 mmol/L）');
    }

    return this.prisma.bloodSugarRecord.update({
      where: { id: recordId },
      data: {
        type: dto.type,
        value: dto.value,
        inputMethod: dto.inputMethod,
        source: dto.source,
        recordedAt: dto.recordedAt ? new Date(dto.recordedAt) : undefined,
      },
    });
  }

  /** 删除血糖记录 */
  async deleteBgRecord(userId: string, recordId: string) {
    const existing = await this.prisma.bloodSugarRecord.findFirst({
      where: { id: recordId, userId },
    });

    if (!existing) {
      throw new NotFoundException('血糖记录不存在');
    }

    await this.prisma.bloodSugarRecord.delete({
      where: { id: recordId },
    });

    return { deleted: true };
  }

  // ==================== 统计接口 ====================

  /** 获取今日指标摘要 */
  async getTodaySummary(userId: string) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const [bpCount, bgCount, latestBp, latestBg] = await Promise.all([
      this.prisma.bloodPressureRecord.count({
        where: {
          userId,
          recordedAt: { gte: today, lt: tomorrow },
        },
      }),
      this.prisma.bloodSugarRecord.count({
        where: {
          userId,
          recordedAt: { gte: today, lt: tomorrow },
        },
      }),
      this.prisma.bloodPressureRecord.findFirst({
        where: { userId },
        orderBy: { recordedAt: 'desc' },
      }),
      this.prisma.bloodSugarRecord.findFirst({
        where: { userId },
        orderBy: { recordedAt: 'desc' },
      }),
    ]);

    return {
      todayBpCount: bpCount,
      todayBgCount: bgCount,
      latestBp: latestBp
        ? {
            systolic: latestBp.systolic,
            diastolic: latestBp.diastolic,
            heartRate: latestBp.heartRate,
            recordedAt: latestBp.recordedAt,
          }
        : null,
      latestBg: latestBg
        ? {
            type: latestBg.type,
            value: latestBg.value,
            recordedAt: latestBg.recordedAt,
          }
        : null,
    };
  }

  /** 获取最近N天的血压趋势数据 */
  async getBpTrend(userId: string, days: number = 7) {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);
    startDate.setHours(0, 0, 0, 0);

    const records = await this.prisma.bloodPressureRecord.findMany({
      where: {
        userId,
        recordedAt: { gte: startDate },
      },
      orderBy: { recordedAt: 'asc' },
      select: {
        systolic: true,
        diastolic: true,
        heartRate: true,
        recordedAt: true,
      },
    });

    // 按日期分组
    const grouped: Record<string, typeof records> = {};
    for (const record of records) {
      const dateKey = record.recordedAt.toISOString().split('T')[0];
      if (!grouped[dateKey]) grouped[dateKey] = [];
      grouped[dateKey].push(record);
    }

    // 计算每日平均值
    const trend = Object.entries(grouped).map(([date, dayRecords]) => ({
      date,
      avgSystolic: Math.round(
        dayRecords.reduce((sum, r) => sum + r.systolic, 0) / dayRecords.length,
      ),
      avgDiastolic: Math.round(
        dayRecords.reduce((sum, r) => sum + r.diastolic, 0) / dayRecords.length,
      ),
      avgHeartRate: dayRecords[0].heartRate
        ? Math.round(
            dayRecords
              .filter((r) => r.heartRate)
              .reduce((sum, r) => sum + (r.heartRate || 0), 0) /
              dayRecords.filter((r) => r.heartRate).length,
          )
        : null,
      count: dayRecords.length,
    }));

    return trend;
  }

  /** 获取最近N天的血糖趋势数据 */
  async getBgTrend(userId: string, days: number = 7) {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);
    startDate.setHours(0, 0, 0, 0);

    const records = await this.prisma.bloodSugarRecord.findMany({
      where: {
        userId,
        recordedAt: { gte: startDate },
      },
      orderBy: { recordedAt: 'asc' },
      select: {
        type: true,
        value: true,
        recordedAt: true,
      },
    });

    // 按日期分组
    const grouped: Record<string, typeof records> = {};
    for (const record of records) {
      const dateKey = record.recordedAt.toISOString().split('T')[0];
      if (!grouped[dateKey]) grouped[dateKey] = [];
      grouped[dateKey].push(record);
    }

    // 计算每日平均值
    const trend = Object.entries(grouped).map(([date, dayRecords]) => ({
      date,
      avgValue: parseFloat(
        (
          dayRecords.reduce((sum, r) => sum + Number(r.value), 0) /
          dayRecords.length
        ).toFixed(1),
      ),
      count: dayRecords.length,
      byType: this.groupBgByType(dayRecords),
    }));

    return trend;
  }

  // ==================== B4-4: 趋势数据接口 ====================

  async getTrendData(
    userId: string,
    type: 'bp' | 'bg',
    period: string,
    granularity: 'day' | 'week' | 'month',
  ) {
    const periodDays = period === '90d' ? 90 : period === '30d' ? 30 : 7;
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - periodDays);
    startDate.setHours(0, 0, 0, 0);
    const endDate = new Date();
    endDate.setHours(23, 59, 59, 999);

    if (type === 'bp') {
      const records = await this.prisma.bloodPressureRecord.findMany({
        where: { userId, recordedAt: { gte: startDate, lte: endDate } },
        orderBy: { recordedAt: 'asc' },
      });

      if (granularity === 'day') {
        return this.aggregateByDay(records, (r) => ({
          systolic: r.systolic,
          diastolic: r.diastolic,
          heartRate: r.heartRate,
        }));
      } else if (granularity === 'week') {
        return this.aggregateByWeek(records, (r) => ({
          systolic: r.systolic,
          diastolic: r.diastolic,
          heartRate: r.heartRate,
        }));
      } else {
        return this.aggregateByMonth(records, (r) => ({
          systolic: r.systolic,
          diastolic: r.diastolic,
          heartRate: r.heartRate,
        }));
      }
    } else {
      const records = await this.prisma.bloodSugarRecord.findMany({
        where: { userId, recordedAt: { gte: startDate, lte: endDate } },
        orderBy: { recordedAt: 'asc' },
      });

      if (granularity === 'day') {
        return this.aggregateBgByDay(records);
      } else if (granularity === 'week') {
        return this.aggregateBgByWeek(records);
      } else {
        return this.aggregateBgByMonth(records);
      }
    }
  }

  private aggregateByDay<T extends { recordedAt: Date }>(
    records: T[],
    extract: (r: T) => { systolic: number; diastolic: number; heartRate?: number | null },
  ) {
    const grouped: Record<string, T[]> = {};
    for (const r of records) {
      const key = r.recordedAt.toISOString().split('T')[0];
      if (!grouped[key]) grouped[key] = [];
      grouped[key].push(r);
    }

    return Object.entries(grouped).map(([date, recs]) => {
      const vals = recs.map(extract);
      const avgSystolic = Math.round(vals.reduce((s, v) => s + v.systolic, 0) / vals.length);
      const avgDiastolic = Math.round(vals.reduce((s, v) => s + v.diastolic, 0) / vals.length);
      return {
        date,
        avgSystolic,
        avgDiastolic,
        movingAvgSystolic: avgSystolic,
        movingAvgDiastolic: avgDiastolic,
        count: recs.length,
      };
    });
  }

  private aggregateByWeek<T extends { recordedAt: Date }>(
    records: T[],
    extract: (r: T) => { systolic: number; diastolic: number; heartRate?: number | null },
  ) {
    const weekMap: Record<string, T[]> = {};
    for (const r of records) {
      const d = r.recordedAt;
      const weekStart = new Date(d);
      weekStart.setDate(d.getDate() - d.getDay());
      const key = weekStart.toISOString().split('T')[0];
      if (!weekMap[key]) weekMap[key] = [];
      weekMap[key].push(r);
    }

    const sorted = Object.entries(weekMap).sort(([a], [b]) => a.localeCompare(b));
    let prevSystolic = 0;
    let prevDiastolic = 0;

    return sorted.map(([weekStart, recs]) => {
      const vals = recs.map(extract);
      const avgSystolic = Math.round(vals.reduce((s, v) => s + v.systolic, 0) / vals.length);
      const avgDiastolic = Math.round(vals.reduce((s, v) => s + v.diastolic, 0) / vals.length);
      const movingAvgSystolic = prevSystolic ? Math.round((avgSystolic + prevSystolic) / 2) : avgSystolic;
      const movingAvgDiastolic = prevDiastolic ? Math.round((avgDiastolic + prevDiastolic) / 2) : avgDiastolic;
      prevSystolic = avgSystolic;
      prevDiastolic = avgDiastolic;
      return { weekStart, avgSystolic, avgDiastolic, movingAvgSystolic, movingAvgDiastolic, count: recs.length };
    });
  }

  private aggregateByMonth<T extends { recordedAt: Date }>(
    records: T[],
    extract: (r: T) => { systolic: number; diastolic: number; heartRate?: number | null },
  ) {
    const monthMap: Record<string, T[]> = {};
    for (const r of records) {
      const key = r.recordedAt.toISOString().slice(0, 7);
      if (!monthMap[key]) monthMap[key] = [];
      monthMap[key].push(r);
    }

    const sorted = Object.entries(monthMap).sort(([a], [b]) => a.localeCompare(b));
    let prevSystolic = 0;
    let prevDiastolic = 0;

    return sorted.map(([month, recs]) => {
      const vals = recs.map(extract);
      const avgSystolic = Math.round(vals.reduce((s, v) => s + v.systolic, 0) / vals.length);
      const avgDiastolic = Math.round(vals.reduce((s, v) => s + v.diastolic, 0) / vals.length);
      const movingAvgSystolic = prevSystolic ? Math.round((avgSystolic + prevSystolic) / 2) : avgSystolic;
      const movingAvgDiastolic = prevDiastolic ? Math.round((avgDiastolic + prevDiastolic) / 2) : avgDiastolic;
      prevSystolic = avgSystolic;
      prevDiastolic = avgDiastolic;
      return { month, avgSystolic, avgDiastolic, movingAvgSystolic, movingAvgDiastolic, count: recs.length };
    });
  }

  private aggregateBgByDay(records: any[]) {
    const grouped: Record<string, any[]> = {};
    for (const r of records) {
      const key = r.recordedAt.toISOString().split('T')[0];
      if (!grouped[key]) grouped[key] = [];
      grouped[key].push(r);
    }

    return Object.entries(grouped).map(([date, recs]) => ({
      date,
      avgValue: parseFloat((recs.reduce((s, r) => s + Number(r.value), 0) / recs.length).toFixed(1)),
      count: recs.length,
      byType: this.groupBgByType(recs),
    }));
  }

  private aggregateBgByWeek(records: any[]) {
    const weekMap: Record<string, any[]> = {};
    for (const r of records) {
      const d = r.recordedAt;
      const weekStart = new Date(d);
      weekStart.setDate(d.getDate() - d.getDay());
      const key = weekStart.toISOString().split('T')[0];
      if (!weekMap[key]) weekMap[key] = [];
      weekMap[key].push(r);
    }

    return Object.entries(weekMap)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([weekStart, recs]) => ({
        weekStart,
        avgValue: parseFloat((recs.reduce((s, r) => s + Number(r.value), 0) / recs.length).toFixed(1)),
        count: recs.length,
        byType: this.groupBgByType(recs),
      }));
  }

  private aggregateBgByMonth(records: any[]) {
    const monthMap: Record<string, any[]> = {};
    for (const r of records) {
      const key = r.recordedAt.toISOString().slice(0, 7);
      if (!monthMap[key]) monthMap[key] = [];
      monthMap[key].push(r);
    }

    return Object.entries(monthMap)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([month, recs]) => ({
        month,
        avgValue: parseFloat((recs.reduce((s, r) => s + Number(r.value), 0) / recs.length).toFixed(1)),
        count: recs.length,
        byType: this.groupBgByType(recs),
      }));
  }

  // ==================== B4-5: 服药依从性趋势 ====================

  async getAdherenceTrend(userId: string, month: string) {
    const [year, mon] = month.split('-').map(Number);
    const startDate = new Date(year, mon - 1, 1);
    const endDate = new Date(year, mon, 0, 23, 59, 59, 999);

    const records = await this.prisma.medicationRecord.findMany({
      where: { userId, scheduledTime: { gte: startDate, lte: endDate } },
      orderBy: { scheduledTime: 'asc' },
    });

    const dailyMap: Record<string, { taken: number; late: number; missed: number; total: number }> = {};
    for (let d = 1; d <= endDate.getDate(); d++) {
      const dateKey = `${year}-${String(mon).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      dailyMap[dateKey] = { taken: 0, late: 0, missed: 0, total: 0 };
    }

    for (const r of records) {
      const dateKey = r.scheduledTime.toISOString().split('T')[0];
      if (!dailyMap[dateKey]) {
        dailyMap[dateKey] = { taken: 0, late: 0, missed: 0, total: 0 };
      }
      const day = dailyMap[dateKey];
      day.total++;
      if (r.status === 'taken') {
        if (r.actualTime && r.actualTime > r.scheduledTime) {
          day.late++;
        } else {
          day.taken++;
        }
      } else if (r.status === 'missed') {
        day.missed++;
      } else if (r.status === 'skipped') {
        day.missed++;
      }
    }

    const totalScheduled = records.length;
    const totalTaken = records.filter((r) => r.status === 'taken').length;
    const totalLate = records.filter((r) => r.status === 'taken' && r.actualTime && r.actualTime > r.scheduledTime).length;
    const totalMissed = records.filter((r) => r.status === 'missed' || r.status === 'skipped').length;

    const monthRate = totalScheduled > 0 ? Math.round((totalTaken / totalScheduled) * 100) : 0;

    return {
      month,
      overallRate: monthRate,
      totalRecords: totalScheduled,
      taken: totalTaken - totalLate,
      late: totalLate,
      missed: totalMissed,
      daily: Object.entries(dailyMap).map(([date, data]) => ({
        date,
        taken: data.taken,
        late: data.late,
        missed: data.missed,
        total: data.total,
        rate: data.total > 0 ? Math.round(((data.taken + data.late) / data.total) * 100) : 0,
      })),
    };
  }

  /** 按类型分组血糖数据 */
  private groupBgByType(records: { type: string; value: any }[]) {
    const grouped: Record<string, number[]> = {};
    for (const record of records) {
      if (!grouped[record.type]) grouped[record.type] = [];
      grouped[record.type].push(Number(record.value));
    }

    return Object.entries(grouped).map(([type, values]) => ({
      type,
      avgValue: parseFloat(
        (values.reduce((a, b) => a + b, 0) / values.length).toFixed(1),
      ),
      count: values.length,
    }));
  }
}
