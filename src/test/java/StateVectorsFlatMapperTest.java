import org.apache.flink.util.Collector;
import org.junit.Test;
import org.mockito.Mockito;
import org.msergo.models.StateVector;
import org.msergo.models.StateVectorsResponse;
import org.msergo.transforms.StateVectorsFlatMapper;

import java.util.Collection;
import java.util.Collections;

import static org.mockito.Mockito.mock;

public class StateVectorsFlatMapperTest {
    @Test
    public void testStateVectorsFlatMapper() throws Exception {
        StateVectorsFlatMapper stateVectorsFlatMapper = new StateVectorsFlatMapper();
        Collector<StateVector> collector = mock(Collector.class);

        StateVectorsResponse stateVectorsResponse1 = new StateVectorsResponse();
        StateVector stateVector = new StateVector("icao0000");
        StateVectorsResponse stateVectorsResponse = new StateVectorsResponse();
        stateVectorsResponse.setStates(Collections.singletonList(stateVector));
        Collection<StateVector> stateVectorsResponse0 = Collections.singletonList(stateVector);

        stateVectorsResponse1.setStates(stateVectorsResponse0);

        stateVectorsFlatMapper.flatMap(stateVectorsResponse1, collector);
        Mockito.verify(collector, Mockito.times(1)).collect(stateVector);
    }
}
