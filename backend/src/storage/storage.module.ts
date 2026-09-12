import { Module } from '@nestjs/common';
import { StorageService } from './storage.service.js';
import { PhotosController } from './photos.controller.js';

@Module({
  providers: [StorageService],
  controllers: [PhotosController],
  exports: [StorageService],
})
export class StorageModule {}
