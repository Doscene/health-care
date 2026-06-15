import { ApiProperty } from '@nestjs/swagger';
import { IsString, Matches, Length } from 'class-validator';

export class SendCodeDto {
  @ApiProperty({ example: '13800138000', description: '手机号' })
  @IsString()
  @Matches(/^1[3-9]\d{9}$/, { message: '手机号格式不正确' })
  phone: string;
}

export class LoginDto {
  @ApiProperty({ example: '13800138000', description: '手机号' })
  @IsString()
  @Matches(/^1[3-9]\d{9}$/, { message: '手机号格式不正确' })
  phone: string;

  @ApiProperty({ example: '123456', description: '验证码' })
  @IsString()
  @Length(6, 6, { message: '验证码为6位数字' })
  @Matches(/^\d{6}$/, { message: '验证码为6位数字' })
  code: string;
}

export class TokenResponseDto {
  @ApiProperty()
  accessToken: string;

  @ApiProperty()
  refreshToken: string;

  @ApiProperty({ description: '用户ID（新用户自动注册时也会返回）' })
  userId: string;
}
