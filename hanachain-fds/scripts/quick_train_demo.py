"""
Quick training demo - shortened version for testing.
"""

import sys
from pathlib import Path
import pandas as pd

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.dqn_model import DQNAgent
from src.reward_function import RewardFunction
from src.trainer import DQNTrainer, TrainingPhase
from src.logger import setup_logger

logger = setup_logger(__name__, log_level="INFO", log_file="quick_train_demo")


def main():
    """Quick training demo with reduced epochs."""
    logger.info("=" * 80)
    logger.info("QUICK DQN TRAINING DEMO")
    logger.info("=" * 80)

    # Load training data
    logger.info("\n>>> Loading Training Data...")
    data_dir = Path("data/processed")
    train_df = pd.read_parquet(data_dir / "train_processed.parquet")

    # Use only 10K samples for quick demo
    train_df = train_df.sample(n=10000, random_state=42)

    logger.info(f"Loaded {len(train_df):,} training samples (sampled for demo)")
    logger.info(f"Features: {len([c for c in train_df.columns if c != 'is_fraud'])}")
    logger.info(f"Fraud rate: {train_df['is_fraud'].mean():.2%}")

    # Initialize components
    logger.info("\n>>> Initializing DQN Components...")

    input_dim = len([c for c in train_df.columns if c != 'is_fraud'])
    agent = DQNAgent(
        input_dim=input_dim,
        gamma=0.99,
        epsilon_start=1.0,
        epsilon_end=0.1,
        epsilon_decay_steps=1000,
        target_update_freq=50
    )
    logger.info(f"Created DQN Agent with {input_dim} input features")

    reward_fn = RewardFunction()
    logger.info("Created RewardFunction")

    trainer = DQNTrainer(
        agent=agent,
        reward_function=reward_fn,
        replay_buffer_size=10000,
        batch_size=32
    )

    # Override with shorter phases for demo
    trainer.phases = [
        TrainingPhase("Quick Demo Phase", epochs=5, epsilon_start=1.0, epsilon_end=0.1)
    ]

    logger.info("Created DQNTrainer with quick demo phases")
    logger.info(f"Demo: 1 phase × 5 epochs = 5 total epochs")

    # Train model
    logger.info("\n>>> Starting Quick Training Demo...")
    trainer.train(
        train_data=train_df,
        steps_per_epoch=20,
        save_checkpoints=True,
        checkpoint_freq=2
    )

    # Print summary
    logger.info("\n>>> Training Summary:")
    summary = trainer.get_metrics_summary()
    for key, value in summary.items():
        if isinstance(value, float):
            logger.info(f"  {key}: {value:.4f}")
        else:
            logger.info(f"  {key}: {value}")

    # Show final action distribution
    logger.info("\n>>> Final Model Behavior:")
    final_epoch = trainer.training_history[-1]
    logger.info(f"  APPROVE: {final_epoch['action_approve']:.2%}")
    logger.info(f"  MANUAL_REVIEW: {final_epoch['action_review']:.2%}")
    logger.info(f"  BLOCK: {final_epoch['action_block']:.2%}")

    logger.info("\n" + "=" * 80)
    logger.info("QUICK DEMO COMPLETE")
    logger.info("=" * 80)
    logger.info("\nCheckpoint saved to: data/models/checkpoints/")
    logger.info("Training history saved to: data/models/logs/training_history.csv")


if __name__ == "__main__":
    main()
