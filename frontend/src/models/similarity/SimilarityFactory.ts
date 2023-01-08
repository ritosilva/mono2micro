import Similarity from "./Similarity";
import SimilaritySciPyWeights, {SIMILARITY_SCIPY_WEIGHTS} from "./SimilaritySciPyWeights";
import SimilarityMatrixEntityVectorization, {SIMILARITY_SCIPY_ENTITY_VECTORIZATION} from "./SimilarityMatrixEntityVectorization";
import SimilarityMatrixClassVectorization, {SIMILARITY_SCIPY_CLASS_VECTORIZATION} from "./SimilarityMatrixClassVectorization";

export abstract class SimilarityFactory {
    static getSimilarity(similarity: any) : Similarity {
        switch (similarity.type) {
            case SIMILARITY_SCIPY_WEIGHTS:
                return new SimilaritySciPyWeights(similarity);
            case SIMILARITY_SCIPY_ENTITY_VECTORIZATION:
                return new SimilarityMatrixEntityVectorization(similarity);
            case SIMILARITY_SCIPY_CLASS_VECTORIZATION:
                return new SimilarityMatrixClassVectorization(similarity);
            default:
                throw new Error('Type ' + similarity.type + ' unknown.');
        }
    }
}
