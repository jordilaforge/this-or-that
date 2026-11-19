<template>
    <section id="vote">
        <section class="hero is-info is-bold is-fullheight">
            <div class="hero-head">
                <nav class="navbar" role="navigation">
                    <div class="navbar-brand">
                        <div class="navbar-item">
                            <button class="button is-info is-inverted is-outlined" @click="back()">
                                ⬅️ Back
                            </button>
                        </div>
                        <div class="navbar-item"
                             style="flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                            <span class="title"
                                  v-if="voteResponse"
                                  style="margin-bottom: 0; white-space: nowrap;overflow: hidden; text-overflow: ellipsis;">
                                {{ voteResponse.perspective }}
                                <small v-if="isExhaustive">({{ voteResponse.completedComparisons }}/{{ voteResponse.totalComparisons }} Comparisons, SurveyCode: {{ $route.params.surveyCode }})</small>
                                <small v-else>(Votes: {{ submittedVotes }} SurveyCode: {{ $route.params.surveyCode }})</small>
                            </span>
                        </div>
                    </div>
                </nav>
            </div>
            <div class="hero-body">
                <div class="container has-text-centered">

                    <!-- Progress bar in exhaustive mode -->
                    <div v-if="isExhaustive && !isSurveyStopped" class="progress-wrapper columns is-centered">
                        <div class="column is-three-quarters-tablet is-half-desktop">
                            <b-progress
                                type="is-success"
                                size="is-medium"
                                :value="voteResponse.completedComparisons"
                                :max="voteResponse.totalComparisons"
                                show-value
                                format="percent">
                            </b-progress>
                            <p class="has-text-white is-size-6 mt-1" v-if="!isCompleted">
                                Comparison {{ currentComparisonNumber }} of {{ voteResponse.totalComparisons }}
                            </p>
                            <p class="has-text-white is-size-6 mt-1" v-else>
                                All {{ voteResponse.totalComparisons }} comparisons completed (100%)
                            </p>
                        </div>
                    </div>

                    <!-- Images for voting -->
                    <div v-if="voteResponse && voteResponse.id1 != null && !isSurveyStopped && !isCompleted">
                        <div class="image-container columns is-centered" :class="isLandscape ? 'is-mobile':''">
                            <div class="column is-half">
                                <VoteImage :src="getImageURL1()" @click.native="vote(1)"></VoteImage>
                            </div>
                            <div class="column is-half">
                                <VoteImage :src="getImageURL2()" @click.native="vote(2)"></VoteImage>
                            </div>
                        </div>
                    </div>

                    <!-- Exhaustive Mode: All comparisons completed -->
                    <div class="columns is-centered has-text-centered"
                         v-if="isCompleted">
                        <div class="column">
                            <h1 class="title">All comparisons done! 🎉</h1>
                            <h1 class="subtitle">You have completed all image comparisons for this survey. Thank you for voting!</h1>

                            <b-button type="is-info"
                                      size="is-medium"
                                      inverted
                                      @click="manageSurvey()">
                                View survey results
                            </b-button>
                        </div>
                    </div>

                    <!-- Survey Stopped / Finished -->
                    <div class="columns is-centered has-text-centered"
                         v-if="isSurveyStopped && !isCompleted">
                        <div class="column">
                            <h1 class="title">Its over ...</h1>
                            <h1 class="subtitle">... survey has already finished, but you can view the results</h1>

                            <b-button type="is-info"
                                      size="is-medium"
                                      inverted
                                      @click="manageSurvey()">
                                View survey results
                            </b-button>
                        </div>
                    </div>

                </div>

            </div>
            <b-loading :active.sync="isLoading" :can-cancel="true" :is-full-page="true"></b-loading>
        </section>
    </section>
</template>

<script lang="ts">
    import {Component, Vue} from 'vue-property-decorator';
    import {getImageURL, getVote, setVote} from '@/app/api/survey.api';
    import {VoteRequest} from '@/app/models/vote-request';
    import {VoteResponse} from '@/app/models/vote-response';
    import VoteImage from '@/app/components/VoteImage.vue';

    @Component({
        components: {VoteImage},
    })
    export default class VoteContainer extends Vue {
        public isLoading = true;
        public isOver = false;
        public isLandscape = false;
        public voteResponse: VoteResponse | null = null;
        public submittedVotes: number = 0;

        public get isExhaustive(): boolean {
            return !!(
                this.voteResponse &&
                this.voteResponse.votingMode === 'EXHAUSTIVE' &&
                this.voteResponse.totalComparisons != null &&
                this.voteResponse.totalComparisons > 0
            );
        }

        public get isCompleted(): boolean {
            return !!(
                this.voteResponse &&
                (this.voteResponse.completed === true ||
                    (this.isExhaustive && this.voteResponse.id1 == null))
            );
        }

        public get isSurveyStopped(): boolean {
            return this.isOver || (!!this.voteResponse && this.voteResponse.id1 == null && !this.isCompleted);
        }

        public get currentComparisonNumber(): number {
            if (!this.voteResponse || this.voteResponse.completedComparisons == null) {
                return 1;
            }
            const total = this.voteResponse.totalComparisons || 1;
            return Math.min(this.voteResponse.completedComparisons + 1, total);
        }

        public getImageURL1() {
            if (this.voteResponse != null && this.voteResponse.id1 != null) {
                return getImageURL(
                    this.$route.params.surveyCode,
                    this.voteResponse.id1,
                );
            }
            return null;
        }

        public getImageURL2() {
            if (this.voteResponse != null && this.voteResponse.id2 != null) {
                return getImageURL(
                    this.$route.params.surveyCode,
                    this.voteResponse.id2,
                );
            }
            return null;
        }

        public async mounted() {
            this.isLoading = true;
            this.isLandscape = window.innerHeight < window.innerWidth;
            window.addEventListener('resize', () => {
                this.isLandscape = window.innerHeight < window.innerWidth;
            }, false);
            try {
                this.voteResponse = await getVote(this.$route.params.surveyCode);
                if (
                    localStorage.getItem(
                        `this-or-that:${this.$route.params.surveyCode}`,
                    ) !== null
                ) {
                    this.submittedVotes = parseInt(
                        localStorage.getItem(
                            `this-or-that:${this.$route.params.surveyCode}`,
                        )!,
                        10,
                    );
                }
            } catch (error) {
                if (
                    'response' in error &&
                    'status' in error.response &&
                    error.response.status === 404
                ) {
                    this.$router.push('/404');
                    return;
                }
                if (
                    'response' in error &&
                    'status' in error.response &&
                    error.response.status === 400
                ) {
                    this.isOver = true;
                    return;
                }
                throw error;
            } finally {
                this.isLoading = false;
            }
        }

        public async vote(selectedImage: number) {
            this.isLoading = true;
            try {
                if (this.voteResponse != null && this.voteResponse.id1 != null && this.voteResponse.id2 != null) {
                    const winner =
                        selectedImage === 1
                            ? this.voteResponse.id1
                            : this.voteResponse.id2;
                    const loser =
                        selectedImage === 1
                            ? this.voteResponse.id2
                            : this.voteResponse.id1;
                    const voteRequest: VoteRequest = {winner, loser};
                    await setVote(this.$route.params.surveyCode, voteRequest);
                    this.voteResponse = await getVote(
                        this.$route.params.surveyCode,
                    );
                    this.submittedVotes += 1;
                    localStorage.setItem(
                        `this-or-that:${this.$route.params.surveyCode}`,
                        this.submittedVotes.toString(),
                    );
                }
            } catch (error) {
                if (
                    'response' in error &&
                    'status' in error.response &&
                    error.response.status === 404
                ) {
                    this.$router.push('/404');
                    return;
                }
                if (
                    'response' in error &&
                    'status' in error.response &&
                    error.response.status === 400
                ) {
                    this.isOver = true;
                    return;
                }
                throw error;
            } finally {
                this.isLoading = false;
            }
        }

        public manageSurvey() {
            this.$router.push('admin');
        }

        public back() {
            this.$router.push('/');
        }
    }
</script>

<style lang="scss" scoped>
    @import "~bulma/sass/utilities/all";

    .hero-body, .section {
        padding: 1.5rem;
    }

    .progress-wrapper {
        margin-bottom: 1.5rem;
    }

    .image-container {
        .column {
            justify-content: center;
            align-items: center;
            display: flex;
        }

    }

    @include mobile() {
        .title {
            font-size: 1.2rem;
            margin-bottom: 0.75rem;
        }

        .subtitle {
            font-size: 1rem;
        }
    }
</style>
