import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { randomUUID } from 'node:crypto';
import { mkdir, writeFile } from 'node:fs/promises';
import { basename, join, resolve } from 'node:path';

@Injectable()
export class StorageService {
  private readonly dir: string;

  constructor(config: ConfigService) {
    this.dir = resolve(config.get<string>('STORAGE_DIR') ?? './data/photos');
  }

  async savePhoto(
    buffer: Buffer,
    mimeType: string,
  ): Promise<{ filename: string; url: string }> {
    await mkdir(this.dir, { recursive: true });
    const extension = mimeType === 'image/png' ? 'png' : 'jpg';
    const filename = `${randomUUID()}.${extension}`;
    await writeFile(join(this.dir, filename), buffer);
    return { filename, url: `/api/vin/photos/${filename}` };
  }

  /** Resolves a stored filename to an absolute path, guarding against path traversal. */
  resolvePath(filename: string): string {
    return join(this.dir, basename(filename));
  }
}
