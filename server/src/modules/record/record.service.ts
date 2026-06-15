import { Injectable, BadRequestException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class RecordService {
  constructor(private readonly prisma: PrismaService) {}

  // ==================== 血压记录 ====================

  async addBpRecord(
    userId: string,
    data: { systolic: number; diastolic: number; heartRate?: number; inputMethod?: string },
  ) {
    if (data.systolic < 50 || data.systolic > 300) {
      throw new BadRequestException('收缩压数值异常');
    }
    if (data.diastolic < 30 || data.diastolic > 200) {
      throw new BadRequestException('舒张压数值异常');
    }

    return this.prisma.bloodPressureRecord.create({
      data: {
        id: uuidv4(),
        userId,
        systolic: data.systolic,
        diastolic: data.diastolic,
        heartRate: data.heartRate ?? null,
        inputMethod: data.inputMethod ?? 'manual',
      },
    });
  }

  async getBpRecords(userId: string, limit = 30) {
    return this.prisma.bloodPressureRecord.findMany({
      where: { userId },
      orderBy: { recordedAt: 'desc' },
      take: limit,
    });
  }

  async deleteBpRecord(userId: string, recordId: string) {
    const record = await this.prisma.bloodPressureRecord.findFirst({
      where: { id: recordId, userId },
    });
    if (!record) throw new NotFoundException('记录不存在');

    await this.prisma.bloodPressureRecord.delete({ where: { id: recordId } });
    return { message: '已删除' };
  }

  // ==================== 血糖记录 ====================

  async addBgRecord(
    userId: string,
    data: { type: string; value: number; inputMethod?: string },
  ) {
    const validTypes = ['fasting', 'before_meal', 'after_meal', 'before_sleep', 'random'];
    if (!validTypes.includes(data.type)) {
      throw new BadRequestException(`无效的血糖类型，允许值：${validTypes.join(', ')}`);
    }
    if (data.value < 1 || data.value > 50) {
      throw new BadRequestException('血糖数值异常');
    }

    return this.prisma.bloodSugarRecord.create({
      data: {
        id: uuidv4(),
        userId,
        type: data.type,
        value: data.value,
        inputMethod: data.inputMethod ?? 'manual',
      },
    });
  }

  async getBgRecords(userId: string, limit = 30) {
    return this.prisma.bloodSugarRecord.findMany({
      where: { userId },
      orderBy: { recordedAt: 'desc' },
      take: limit,
    });
  }

  async deleteBgRecord(userId: string, recordId: string) {
    const record = await this.prisma.bloodSugarRecord.findFirst({
      where: { id: recordId, userId },
    });
    if (!record) throw new NotFoundException('记录不存在');

    await this.prisma.bloodSugarRecord.delete({ where: { id: recordId } });
    return { message: '已删除' };
  }
}
