<template>
    <div class="review-dialog">
        <el-dialog
            :title="title"
            :visible.sync="dialogVisible"
            width="600px"
            :close-on-click-modal="false"
            @close="handleClose"
        >
            <div class="review-form">
                <!-- 商品信息 -->
                <div class="good-info" v-if="goodInfo">
                    <img :src="baseApi + goodInfo.imgs" alt="商品图片" class="good-image" />
                    <div class="good-detail">
                        <div class="good-name">{{ goodInfo.name }}</div>
                        <div class="good-standard">{{ standard }}</div>
                    </div>
                </div>

                <!-- 评分 -->
                <div class="form-item">
                    <label class="label">总体评分</label>
                    <div class="rating-selector">
                        <span
                            v-for="star in 5"
                            :key="star"
                            class="star"
                            :class="{ active: star <= rating }"
                            @click="rating = star"
                        >
                            {{ star <= rating ? '⭐' : '☆' }}
                        </span>
                        <span class="rating-text">{{ ratingText }}</span>
                    </div>
                </div>

                <!-- 评论内容 -->
                <div class="form-item">
                    <label class="label">评论内容</label>
                    <el-input
                        v-model="content"
                        type="textarea"
                        :rows="5"
                        placeholder="分享您的使用体验，帮助其他买家做出决定..."
                        maxlength="500"
                        show-word-limit
                    ></el-input>
                </div>

                <!-- 评论标签 -->
                <div class="form-item">
                    <label class="label">添加标签</label>
                    <div class="tag-selector">
                        <el-tag
                            v-for="(tag, index) in availableTags"
                            :key="index"
                            :type="selectedTags.includes(tag) ? 'success' : 'info'"
                            effect="plain"
                            closable
                            @click="toggleTag(tag)"
                            @close="removeTag(tag)"
                            class="tag-item"
                        >
                            {{ tag }}
                        </el-tag>
                    </div>
                </div>

                <!-- 上传图片 -->
                <div class="form-item">
                    <label class="label">上传图片</label>
                    <div class="image-uploader">
                        <el-upload
                            action="#"
                            list-type="picture-card"
                            :auto-upload="false"
                            :file-list="imageList"
                            :on-change="handleImageChange"
                            :on-remove="handleImageRemove"
                            :limit="9"
                            accept="image/*"
                        >
                            <i class="el-icon-plus"></i>
                        </el-upload>
                    </div>
                </div>
            </div>

            <div slot="footer" class="dialog-footer">
                <el-button @click="handleClose">取消</el-button>
                <el-button type="primary" @click="submitReview" :loading="submitting">
                    {{ submitting ? '提交中...' : '提交评论' }}
                </el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: 'ReviewDialog',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        title: {
            type: String,
            default: '发表评价'
        },
        goodInfo: {
            type: Object,
            default: null
        },
        orderId: {
            type: [Number, String],
            default: ''
        },
        standard: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            baseApi: this.$store.state.baseApi,
            dialogVisible: this.visible,
            rating: 5,
            content: '',
            selectedTags: [],
            imageList: [],
            submitting: false,
            availableTags: ['质量好', '物流快', '包装精美', '性价比高', '值得购买', '描述相符', '客服态度好', '会回购']
        };
    },
    watch: {
        visible(val) {
            this.dialogVisible = val;
        },
        dialogVisible(val) {
            this.$emit('update:visible', val);
        }
    },
    computed: {
        ratingText() {
            const texts = ['非常差', '差', '一般', '好', '很好', '非常好'];
            return texts[this.rating];
        }
    },
    methods: {
        toggleTag(tag) {
            if (this.selectedTags.includes(tag)) {
                this.removeTag(tag);
            } else {
                if (this.selectedTags.length < 3) {
                    this.selectedTags.push(tag);
                } else {
                    this.$message.warning('最多选择 3 个标签');
                }
            }
        },
        removeTag(tag) {
            const index = this.selectedTags.indexOf(tag);
            if (index > -1) {
                this.selectedTags.splice(index, 1);
            }
        },
        handleImageChange(file, fileList) {
            this.imageList = fileList;
        },
        handleImageRemove(file, fileList) {
            this.imageList = fileList;
        },
        handleClose() {
            this.resetForm();
            this.$emit('update:visible', false);
        },
        resetForm() {
            this.rating = 5;
            this.content = '';
            this.selectedTags = [];
            this.imageList = [];
        },
        submitReview() {
            // 验证
            if (this.rating === 0) {
                this.$message.warning('请选择评分');
                return;
            }
            if (!this.content.trim()) {
                this.$message.warning('请输入评论内容');
                return;
            }

            this.submitting = true;

            // 构建评论数据
            const reviewData = {
                goodId: this.goodInfo ? this.goodInfo.id : null,
                orderId: this.orderId,
                rating: this.rating,
                content: this.content,
                tags: this.selectedTags.join(','),
                // 图片上传逻辑可以后续扩展
                images: null
            };

            // 提交评论
            this.request.post('/api/review', reviewData)
                .then(res => {
                    if (res.code === '200') {
                        this.$message.success('评论提交成功！');
                        this.handleClose();
                        this.$emit('review-submitted');
                    } else {
                        this.$message.error(res.message || '评论提交失败');
                    }
                })
                .catch(error => {
                    console.error('评论提交失败:', error);
                    this.$message.error('评论提交失败，请重试');
                })
                .finally(() => {
                    this.submitting = false;
                });
        }
    }
};
</script>

<style scoped>
.review-form {
    padding: 10px 0;
}

.good-info {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
    border-radius: 12px;
    margin-bottom: 24px;
}

.good-image {
    width: 100px;
    height: 100px;
    object-fit: cover;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.good-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.good-name {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 8px;
}

.good-standard {
    font-size: 14px;
    color: #6b7280;
}

.form-item {
    margin-bottom: 24px;
}

.label {
    display: block;
    font-size: 15px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 12px;
}

.rating-selector {
    display: flex;
    align-items: center;
    gap: 12px;
}

.star {
    font-size: 32px;
    cursor: pointer;
    transition: all 0.3s ease;
    color: #d1d5db;
}

.star:hover,
.star.active {
    color: #fbbf24;
    transform: scale(1.1);
}

.rating-text {
    font-size: 16px;
    font-weight: 600;
    color: #6366f1;
    margin-left: 8px;
}

.tag-selector {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
}

.tag-item {
    cursor: pointer;
    transition: all 0.3s ease;
}

.tag-item:hover {
    transform: translateY(-2px);
}

.image-uploader {
    margin-top: 8px;
}

::v-deep .el-upload-list__item {
    border-radius: 8px !important;
}
</style>
