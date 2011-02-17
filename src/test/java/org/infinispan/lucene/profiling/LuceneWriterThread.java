package org.infinispan.lucene.profiling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.lucene.store.Directory;
import org.ucieffe.model.Text;


/**
 * LuceneWriterThread is going to perform searches on the Directory until it's
 * interrupted. Good for performance comparisons and stress tests. It needs a
 * SharedState object to be shared with other readers and writers on the same
 * directory to be able to throw exceptions in case it's able to detect an
 * illegal state.
 * 
 * @author Sanne Grinovero
 * @author Davide Di Somma
 * @since 4.0
 */
public class LuceneWriterThread extends LuceneUserThread {

	EntityManager entityManager;

	LuceneWriterThread(Directory dir, SharedState state,
			EntityManager entityManager) {
		super(dir, state);
		this.entityManager = entityManager;
	}

	@Override
	protected void testLoop() throws IOException {
		List<Text> texts = new ArrayList<Text>();
		int numElements = state.textsOutOfIndex.drainTo(texts, 2);

		if (numElements == 2) {
			EntityTransaction transaction = entityManager.getTransaction();
			transaction.begin();
			Text text1 = texts.get(0);
			Text text2 = texts.get(1);

			String oldText1 = text1.getText();
			String oldText2 = text2.getText();
			text1.setText(oldText2);
			text2.setText(oldText1);
			
			entityManager.merge(text1);
			entityManager.merge(text2);

			transaction.commit();
			
			state.textsInIndex.addAll(texts);
			state.incrementIndexWriterTaskCount(numElements);
		} else {
//			System.out.println("Only found " + numElements + " elements.");
		}
	}

}
