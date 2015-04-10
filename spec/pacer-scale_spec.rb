$: << 'src/ruby'
require 'spec_helper'

describe 'simple rating scale' do
  before do
    @g = Pacer.tg
    @root = @g.create_vertex(PacerScale::Root)
    @root.generate_scale 'ranking', 1, 10, 1
  end

  it 'should find each valid ranking' do
    v = @root.find 'ranking', 1
    v.should_not be_nil
  end

  it 'should find each valid ranking' do
    (1..10).each do |n|
      v = @root.find 'ranking', n
      v.should_not be_nil
      v.value.should == n
    end
  end
end
