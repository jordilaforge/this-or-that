export class VoteResponse {
    public id1!: string | null;
    public id2!: string | null;
    public surveyIsRunning!: boolean;
    public perspective!: string;
    public votingMode?: 'EXHAUSTIVE' | 'ENDLESS';
    public totalComparisons?: number | null;
    public completedComparisons?: number;
    public completed?: boolean;
}
